package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.SensorEventRequest;
import com.nulla.donghaeguard.dto.response.SensorEventResponse;
import com.nulla.donghaeguard.entity.*;
import com.nulla.donghaeguard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorEventService {

    private final SensorEventRepository sensorEventRepository;
    private final UltrasonicEventRepository ultrasonicEventRepository;
    private final TiltEventRepository tiltEventRepository;
    private final CameraEventRepository cameraEventRepository;
    private final TripRepository tripRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public SensorEventResponse saveSensorEvent(SensorEventRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("디바이스를 찾을 수 없습니다."));

        // 공통 SensorEvent 저장
        SensorEvent sensorEvent = SensorEvent.builder()
                .trip(trip)
                .device(device)
                .sensorType(SensorEvent.SensorType.valueOf(request.getSensorType()))
                .riskLevel(SensorEvent.RiskLevel.valueOf(request.getRiskLevel()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .detectedAt(request.getDetectedAt())
                .build();

        SensorEvent saved = sensorEventRepository.save(sensorEvent);

        // 센서 타입별 세부 데이터 저장
        switch (request.getSensorType()) {
            case "ULTRASONIC" -> {
                UltrasonicEvent ultrasonicEvent = UltrasonicEvent.builder()
                        .sensorEvent(saved)
                        .sensorId(request.getSensorId())
                        .distanceMm(request.getDistanceMm())
                        .build();
                ultrasonicEventRepository.save(ultrasonicEvent);
            }
            case "TILT" -> {
                TiltEvent tiltEvent = TiltEvent.builder()
                        .sensorEvent(saved)
                        .pitch(request.getPitch())
                        .slopeStatus(TiltEvent.SlopeStatus.valueOf(request.getSlopeStatus()))
                        .build();
                tiltEventRepository.save(tiltEvent);
            }
            case "CAMERA" -> {
                CameraEvent cameraEvent = CameraEvent.builder()
                        .sensorEvent(saved)
                        .detectedObject(request.getDetectedObject())
                        .confidence(request.getConfidence())
                        .stdDev(request.getStdDev())
                        .imageUrl(request.getImageUrl())
                        .build();
                cameraEventRepository.save(cameraEvent);
            }
        }

        return new SensorEventResponse(
                saved.getEventId(),
                saved.getSensorType().name(),
                saved.getRiskLevel().name(),
                true
        );
    }

    @Transactional(readOnly = true)
    public List<SensorEvent> getSensorEventsByTripId(Long tripId) {
        return sensorEventRepository.findByTripTripId(tripId);
    }
}