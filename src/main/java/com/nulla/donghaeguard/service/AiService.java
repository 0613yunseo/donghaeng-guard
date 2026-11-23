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

@Service
@RequiredArgsConstructor
public class AiService {

    private final SensorEventRepository sensorEventRepository;
    private final UltrasonicEventRepository ultrasonicEventRepository;
    private final TiltEventRepository tiltEventRepository;
    private final CameraEventRepository cameraEventRepository;
    private final RiskZoneRepository riskZoneRepository;

    @Transactional(readOnly = true)
    public List<AiDataResponse> getAiData() {
        return sensorEventRepository.findAll().stream()
                .map(this::toAiDataResponse)
                .toList();
    }

    private AiDataResponse toAiDataResponse(SensorEvent e) {
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
                UltrasonicEvent u = ultrasonicEventRepository
                        .findBySensorEventEventId(e.getEventId()).orElse(null);
                if (u != null) {
                    sensorId = u.getSensorId();
                    distanceMm = u.getDistanceMm();
                }
            }
            case TILT -> {
                TiltEvent t = tiltEventRepository
                        .findBySensorEventEventId(e.getEventId()).orElse(null);
                if (t != null) {
                    pitch = t.getPitch();
                    slopeStatus = t.getSlopeStatus().name();
                }
            }
            case CAMERA -> {
                CameraEvent c = cameraEventRepository
                        .findBySensorEventEventId(e.getEventId()).orElse(null);
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