package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.controller.LocationController;
import com.nulla.donghaeguard.dto.request.MeasurementRequest;
import com.nulla.donghaeguard.dto.response.AiMeasurementResponse;
import com.nulla.donghaeguard.entity.*;
import com.nulla.donghaeguard.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementService {

    private static final DateTimeFormatter UTC_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final MeasurementRepository measurementRepository;
    private final SensorEventRepository sensorEventRepository;
    private final UltrasonicEventRepository ultrasonicEventRepository;
    private final TiltEventRepository tiltEventRepository;
    private final CameraEventRepository cameraEventRepository;
    private final TripRepository tripRepository;
    private final DeviceRepository deviceRepository;
    private final EntityManager entityManager;
    private final AiPredictionService aiPredictionService;

    // =========================================================
    // 저장: 측정 주기 1회분을 한 트랜잭션으로 저장
    // =========================================================
    @Transactional
    public String saveMeasurement(MeasurementRequest req) {
        if (measurementRepository.existsById(req.measurementId())) {
            return req.measurementId();
        }

        Trip trip = tripRepository.findById(req.tripId())
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));
        Device device = deviceRepository.findById(req.deviceId())
                .orElseThrow(() -> new RuntimeException("디바이스를 찾을 수 없습니다."));

        LocalDateTime detectedAt = LocalDateTime.ofInstant(req.detectedAt(), ZoneOffset.UTC);
        SensorEvent.RiskLevel riskLevel = SensorEvent.RiskLevel.valueOf(req.riskLevel());

        Double latitude = req.latitude();
        Double longitude = req.longitude();
        double[] loc = LocationController.getLatestLocation(req.tripId());
        if (loc[0] != 0.0 && loc[1] != 0.0) {
            latitude = loc[0];
            longitude = loc[1];
        }

        MeasurementRequest.Ultrasonic us = req.ultrasonic();
        MeasurementRequest.Tilt tilt = req.tilt();
        MeasurementRequest.Camera cam = req.camera();

        Measurement measurement = Measurement.builder()
                .measurementId(req.measurementId())
                .trip(trip)
                .device(device)
                .detectedAt(detectedAt)
                .latitude(latitude)
                .longitude(longitude)
                .riskStage(req.riskStage())
                .riskLevel(riskLevel)
                .matchedRules(req.matchedRules() == null ? null : String.join(",", req.matchedRules()))
                .ruleVersion(req.ruleVersion())
                .reason(req.reason())
                .board1Valid(req.board1Valid())
                .board2Valid(req.board2Valid())
                .frontUsValid(us != null ? us.frontValid() : null)
                .rearUsValid(us != null ? us.rearValid() : null)
                .frontUsMinMm(us != null ? us.frontMinMm() : null)
                .rearUsMinMm(us != null ? us.rearMinMm() : null)
                .tiltValid(tilt != null ? tilt.valid() : null)
                .pitchRaw(tilt != null ? tilt.pitchRaw() : null)
                .pitchDiff(tilt != null ? tilt.pitchDiff() : null)
                .cameraValid(cam != null ? cam.valid() : null)
                .objectDetected(cam != null ? cam.objectDetected() : null)
                .detectedObject(cam != null ? cam.detectedObject() : null)
                .confidence(cam != null ? cam.confidence() : null)
                .maxBoxRatio(cam != null ? cam.maxBoxRatio() : null)
                .stdDev(cam != null ? cam.stdDev() : null)
                .imageUrl(cam != null ? cam.imageUrl() : null)
                .build();
        measurementRepository.save(measurement);

        // ----- 초음파: 센서별 1행 -----
        if (us != null && us.readings() != null) {
            for (MeasurementRequest.Reading r : us.readings()) {
                SensorEvent se = saveBaseEvent(measurement, trip, device,
                        SensorEvent.SensorType.ULTRASONIC, riskLevel, detectedAt, latitude, longitude);
                ultrasonicEventRepository.save(UltrasonicEvent.builder()
                        .sensorEvent(se)
                        .sensorId(r.sensorId())
                        .distanceMm(r.distanceMm())
                        .build());
            }
        }

        // ----- 기울기: 유효할 때만 -----
        if (tilt != null && Boolean.TRUE.equals(tilt.valid()) && tilt.pitchDiff() != null) {
            SensorEvent se = saveBaseEvent(measurement, trip, device,
                    SensorEvent.SensorType.TILT, riskLevel, detectedAt, latitude, longitude);
            String slope = tilt.slopeStatus() != null ? tilt.slopeStatus() : "NORMAL";
            tiltEventRepository.save(TiltEvent.builder()
                    .sensorEvent(se)
                    .pitch(tilt.pitchDiff())
                    .slopeStatus(TiltEvent.SlopeStatus.valueOf(slope))
                    .build());
        }

        // ----- 카메라: 객체 검출 시에만 camera_events 생성 -----
        if (cam != null && Boolean.TRUE.equals(cam.objectDetected())) {
            SensorEvent se = saveBaseEvent(measurement, trip, device,
                    SensorEvent.SensorType.CAMERA, riskLevel, detectedAt, latitude, longitude);
            cameraEventRepository.save(CameraEvent.builder()
                    .sensorEvent(se)
                    .detectedObject(cam.detectedObject())
                    .confidence(cam.confidence())
                    .stdDev(cam.stdDev())
                    .maxBoxRatio(cam.maxBoxRatio())
                    .imageUrl(cam.imageUrl())
                    .build());
        }

        // ===== AI 비동기 호출: DB 커밋 이후 실행 =====
        scheduleAiPrediction(req.measurementId(), req.tripId(), req.riskLevel(), tilt, cam);

        return measurement.getMeasurementId();
    }

    /**
     * AI 예측 호출을 트랜잭션 커밋 후에 실행하도록 등록
     *
     * [TILT 모델]
     * - FastAPI가 pitch_raw만 사용 (pitch_diff, slope_status는 학습 입력 아님)
     * - pitchRaw가 null이거나 0.0이면 전송하지 않음 (FastAPI가 0은 422 거부)
     *
     * [CAMERA 모델]
     * - FastAPI가 confidence, std_dev, detected_object만 사용
     * - objectDetected=false이면 전송하지 않음 (FastAPI가 422 거부)
     */
    private void scheduleAiPrediction(String measurementId, Long tripId, String riskLevel,
                                       MeasurementRequest.Tilt tilt, MeasurementRequest.Camera cam) {
        // TILT 피처: pitchRaw가 null이거나 0.0이면 전송하지 않음
        Map<String, Object> tiltFeatures = null;
        if (tilt != null && Boolean.TRUE.equals(tilt.valid())
                && tilt.pitchRaw() != null && tilt.pitchRaw() != 0.0) {
            tiltFeatures = Map.of(
                    "pitch_raw", tilt.pitchRaw()
            );
        }

        // CAMERA 피처: objectDetected=true이고 필수 필드가 있을 때만
        Map<String, Object> cameraFeatures = null;
        if (cam != null && Boolean.TRUE.equals(cam.valid())
                && Boolean.TRUE.equals(cam.objectDetected())
                && cam.detectedObject() != null && !cam.detectedObject().isBlank()) {
            Map<String, Object> cf = new HashMap<>();
            cf.put("object_detected", true);
            cf.put("detected_object", cam.detectedObject());
            cf.put("confidence", cam.confidence());
            cf.put("std_dev", cam.stdDev());
            cameraFeatures = cf;
        }

        // 둘 다 없으면 AI 호출 불필요
        if (tiltFeatures == null && cameraFeatures == null) {
            return;
        }

        final Map<String, Object> finalTilt = tiltFeatures;
        final Map<String, Object> finalCam = cameraFeatures;

        // 트랜잭션 커밋 이후에 AI 호출
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    aiPredictionService.predictAsync(
                            measurementId, tripId, riskLevel, finalTilt, finalCam);
                } catch (Exception e) {
                    log.warn("[AI] 비동기 호출 등록 실패: {}", e.getMessage());
                }
            }
        });
    }

    private SensorEvent saveBaseEvent(Measurement measurement, Trip trip, Device device,
                                      SensorEvent.SensorType type, SensorEvent.RiskLevel riskLevel,
                                      LocalDateTime detectedAt, Double latitude, Double longitude) {
        return sensorEventRepository.save(SensorEvent.builder()
                .measurement(measurement)
                .trip(trip)
                .device(device)
                .sensorType(type)
                .riskLevel(riskLevel)
                .latitude(latitude)
                .longitude(longitude)
                .detectedAt(detectedAt)
                .build());
    }

    // =========================================================
    // AI 조회
    // =========================================================
    @Transactional(readOnly = true)
    public List<AiMeasurementResponse> findForAi(Long tripId, String deviceId) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.measurement_id, m.trip_id, m.device_id, m.detected_at,
                       m.risk_stage, m.risk_level, m.matched_rules, m.rule_version, m.reason,
                       m.latitude, m.longitude,
                       m.board1_valid, m.board2_valid, m.front_us_valid, m.rear_us_valid,
                       m.front_us_min_mm, m.rear_us_min_mm,
                       MAX(CASE WHEN ue.sensor_id = 1 THEN ue.distance_mm END) AS us1_mm,
                       MAX(CASE WHEN ue.sensor_id = 2 THEN ue.distance_mm END) AS us2_mm,
                       MAX(CASE WHEN ue.sensor_id = 3 THEN ue.distance_mm END) AS us3_mm,
                       MAX(CASE WHEN ue.sensor_id = 4 THEN ue.distance_mm END) AS us4_mm,
                       MAX(CASE WHEN ue.sensor_id = 5 THEN ue.distance_mm END) AS us5_mm,
                       m.tilt_valid, m.pitch_raw, m.pitch_diff,
                       MAX(te.slope_status) AS slope_status,
                       m.camera_valid, m.object_detected, m.detected_object, m.confidence,
                       m.max_box_ratio, m.std_dev, m.image_url
                FROM measurements m
                LEFT JOIN sensor_events se     ON se.measurement_id = m.measurement_id
                LEFT JOIN ultrasonic_events ue ON ue.event_id = se.event_id
                LEFT JOIN tilt_events te       ON te.event_id = se.event_id
                WHERE 1 = 1
                """);
        if (tripId != null) sql.append(" AND m.trip_id = :tripId");
        if (deviceId != null) sql.append(" AND m.device_id = :deviceId");
        sql.append(" GROUP BY m.measurement_id ORDER BY m.detected_at, m.measurement_id");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (tripId != null) query.setParameter("tripId", tripId);
        if (deviceId != null) query.setParameter("deviceId", deviceId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<AiMeasurementResponse> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String rules = str(r[6]);
            result.add(new AiMeasurementResponse(
                    str(r[0]), lng(r[1]), str(r[2]), utc(r[3]),
                    integer(r[4]), str(r[5]),
                    rules == null || rules.isBlank() ? List.of() : Arrays.asList(rules.split(",")),
                    str(r[7]), str(r[8]),
                    dbl(r[9]), dbl(r[10]),
                    bool(r[11]), bool(r[12]), bool(r[13]), bool(r[14]),
                    integer(r[15]), integer(r[16]),
                    integer(r[17]), integer(r[18]), integer(r[19]), integer(r[20]), integer(r[21]),
                    bool(r[22]), dbl(r[23]), dbl(r[24]), str(r[25]),
                    bool(r[26]), bool(r[27]), str(r[28]), dbl(r[29]),
                    dbl(r[30]), dbl(r[31]), str(r[32])
            ));
        }
        return result;
    }

    public String toCsv(List<AiMeasurementResponse> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(",",
                "measurement_id", "trip_id", "device_id", "detected_at_utc",
                "risk_stage", "risk_level", "matched_rules", "rule_version", "reason",
                "latitude", "longitude",
                "board1_valid", "board2_valid", "front_us_valid", "rear_us_valid",
                "front_us_min_mm", "rear_us_min_mm",
                "us1_mm", "us2_mm", "us3_mm", "us4_mm", "us5_mm",
                "tilt_valid", "pitch_raw", "pitch_diff", "slope_status",
                "camera_valid", "object_detected", "detected_object", "confidence",
                "max_box_ratio", "std_dev", "image_url")).append("\n");

        for (AiMeasurementResponse r : rows) {
            sb.append(String.join(",",
                    csv(r.measurementId()), csv(r.tripId()), csv(r.deviceId()), csv(r.detectedAt()),
                    csv(r.riskStage()), csv(r.riskLevel()), csv(String.join("|", r.matchedRules())),
                    csv(r.ruleVersion()), csv(r.reason()),
                    csv(r.latitude()), csv(r.longitude()),
                    csv(r.board1Valid()), csv(r.board2Valid()), csv(r.frontUsValid()), csv(r.rearUsValid()),
                    csv(r.frontUsMinMm()), csv(r.rearUsMinMm()),
                    csv(r.us1Mm()), csv(r.us2Mm()), csv(r.us3Mm()), csv(r.us4Mm()), csv(r.us5Mm()),
                    csv(r.tiltValid()), csv(r.pitchRaw()), csv(r.pitchDiff()), csv(r.slopeStatus()),
                    csv(r.cameraValid()), csv(r.objectDetected()), csv(r.detectedObject()), csv(r.confidence()),
                    csv(r.maxBoxRatio()), csv(r.stdDev()), csv(r.imageUrl()))).append("\n");
        }
        return sb.toString();
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Long lng(Object o) { return o == null ? null : ((Number) o).longValue(); }
    private static Integer integer(Object o) { return o == null ? null : ((Number) o).intValue(); }
    private static Double dbl(Object o) { return o == null ? null : ((Number) o).doubleValue(); }

    private static Boolean bool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        if (o instanceof byte[] bytes) return bytes.length > 0 && bytes[0] != 0;
        return Boolean.parseBoolean(o.toString());
    }

    private static String utc(Object o) {
        if (o == null) return null;
        LocalDateTime t;
        if (o instanceof LocalDateTime ldt) t = ldt;
        else if (o instanceof Timestamp ts) t = ts.toLocalDateTime();
        else return o.toString();
        return t.format(UTC_MILLIS);
    }

    private static String csv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
