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
        Float pitch = null;
        String slopeStatus = null;
        String detectedObject = null;
        Float confidence = null;
        Float stdDev = null;
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

    @Transactional
    public int saveAiResult(AiResultRequest request) {
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

        riskZoneRepository.saveAll(zones);

        return zones.size();
    }
}