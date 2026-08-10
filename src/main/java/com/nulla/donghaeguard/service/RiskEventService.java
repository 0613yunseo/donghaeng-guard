package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.RiskEventRequest;
import com.nulla.donghaeguard.dto.response.RiskEventResponse;
import com.nulla.donghaeguard.entity.Device;
import com.nulla.donghaeguard.entity.RiskEvent;
import com.nulla.donghaeguard.entity.Trip;
import com.nulla.donghaeguard.repository.DeviceRepository;
import com.nulla.donghaeguard.repository.RiskEventRepository;
import com.nulla.donghaeguard.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RiskEventService {

    private final RiskEventRepository riskEventRepository;
    private final TripRepository tripRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public RiskEventResponse saveRiskEvent(RiskEventRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("디바이스를 찾을 수 없습니다."));

        RiskEvent riskEvent = RiskEvent.builder()
                .trip(trip)
                .device(device)
                .sensorType(RiskEvent.SensorType.valueOf(request.getSensorType()))
                .riskType(request.getRiskType())
                .riskLevel(RiskEvent.RiskLevel.valueOf(request.getRiskLevel()))
                .distanceMm(request.getDistanceMm())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .detectedAt(request.getDetectedAt())
                .build();

        RiskEvent saved = riskEventRepository.save(riskEvent);

        return new RiskEventResponse(saved.getEventId(), true);
    }
}