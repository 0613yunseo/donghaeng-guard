package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.AiResultRequest;
import com.nulla.donghaeguard.dto.response.AiDataResponse;
import com.nulla.donghaeguard.entity.CameraEvent;
import com.nulla.donghaeguard.entity.RiskZone;
import com.nulla.donghaeguard.entity.SensorEvent;
import com.nulla.donghaeguard.entity.TiltEvent;
import com.nulla.donghaeguard.entity.UltrasonicEvent;
import com.nulla.donghaeguard.repository.CameraEventRepository;
import com.nulla.donghaeguard.repository.RiskZoneRepository;
import com.nulla.donghaeguard.repository.SensorEventRepository;
import com.nulla.donghaeguard.repository.TiltEventRepository;
import com.nulla.donghaeguard.repository.UltrasonicEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final SensorEventRepository sensorEventRepository;
    private final UltrasonicEventRepository ultrasonicEventRepository;
    private final TiltEventRepository tiltEventRepository;
    private final CameraEventRepository cameraEventRepository;
    private final RiskZoneRepository riskZoneRepository;

    /**
     * AI 분석용 전체 센서 이벤트 조회
     * - 센서별 상세 테이블을 한 번씩만 조회해서 eventId 기준으로 매칭 (쿼리 4회)
     * - 기존: 이벤트마다 개별 조회 → 이벤트 수만큼 쿼리 발생 (N+1)
     */
    @Transactional(readOnly = true)
    public List<AiDataResponse> getAiData() {
        List<SensorEvent> events = sensorEventRepository.findAll();

        Map<Long, UltrasonicEvent> usMap = ultrasonicEventRepository.findAll().stream()
                .filter(u -> u.getSensorEvent() != null)
                .collect(Collectors.toMap(u -> u.getSensorEvent().getEventId(), Function.identity(), (a, b) -> a));
        Map<Long, TiltEvent> tiltMap = tiltEventRepository.findAll().stream()
                .filter(t -> t.getSensorEvent() != null)
                .collect(Collectors.toMap(t -> t.getSensorEvent().getEventId(), Function.identity(), (a, b) -> a));
        Map<Long, CameraEvent> camMap = cameraEventRepository.findAll().stream()
                .filter(c -> c.getSensorEvent() != null)
                .collect(Collectors.toMap(c -> c.getSensorEvent().getEventId(), Function.identity(), (a, b) -> a));

        return events.stream()
                .map(e -> toAiDataResponse(e, usMap, tiltMap, camMap))
                .filter(Objects::nonNull)
                .toList();
    }

    private AiDataResponse toAiDataResponse(SensorEvent e,
                                            Map<Long, UltrasonicEvent> usMap,
                                            Map<Long, TiltEvent> tiltMap,
                                            Map<Long, CameraEvent> camMap) {
        Integer sensorId = null;
        Integer distanceMm = null;
        Double pitch = null;
        String slopeStatus = null;
        String detectedObject = null;
        Double confidence = null;
        Double stdDev = null;
        String imageUrl = null;

        switch (e.getSensorType()) {
            case ULTRASONIC -> {
                UltrasonicEvent u = usMap.get(e.getEventId());
                if (u != null) {
                    sensorId = u.getSensorId();
                    distanceMm = u.getDistanceMm();
                }
            }
            case TILT -> {
                TiltEvent t = tiltMap.get(e.getEventId());
                if (t != null) {
                    pitch = t.getPitch();
                    slopeStatus = t.getSlopeStatus() != null ? t.getSlopeStatus().name() : null;
                }
            }
            case CAMERA -> {
                CameraEvent c = camMap.get(e.getEventId());
                if (c != null) {
                    detectedObject = c.getDetectedObject();
                    confidence = c.getConfidence();
                    stdDev = c.getStdDev();
                    imageUrl = c.getImageUrl();
                }
            }
        }

        return new AiDataResponse(
                e.getEventId(),
                e.getSensorType().name(),
                e.getRiskLevel() != null ? e.getRiskLevel().name() : null,
                e.getLatitude(),
                e.getLongitude(),
                e.getDetectedAt(),
                sensorId,
                distanceMm,
                pitch,
                slopeStatus,
                detectedObject,
                confidence,
                stdDev,
                imageUrl
        );
    }

    /**
     * AI 위험 구간 분석 결과 저장 (전체 교체 방식)
     * - 기존 risk_zones 전체 삭제 후 신규 구간 저장
     * - 하나의 트랜잭션: 저장 중 오류 나면 삭제도 롤백되어 기존 구간 유지
     */
    @Transactional
    public int saveAiResult(AiResultRequest request) {
        if (request.getZones() == null) {
            return 0;  // zones 필드 자체가 없으면 기존 데이터 유지
        }

        List<RiskZone> zones = request.getZones().stream()
                .map(z -> RiskZone.builder()
                        .latitude(z.getLatitude())
                        .longitude(z.getLongitude())
                        .riskScore(z.getRiskScore())
                        .riskGrade(RiskZone.RiskGrade.valueOf(z.getRiskGrade()))
                        .zoneEventCount(z.getZoneEventCount())
                        .reason(z.getReason())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();

        // 기존 위험 구간 전체 삭제 후 신규 저장
        riskZoneRepository.deleteAllInBatch();
        riskZoneRepository.saveAll(zones);

        return zones.size();
    }
}