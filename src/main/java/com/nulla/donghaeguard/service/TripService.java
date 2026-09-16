package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.TripEndRequest;
import com.nulla.donghaeguard.dto.request.TripStartRequest;
import com.nulla.donghaeguard.dto.response.*;
import com.nulla.donghaeguard.entity.*;
import com.nulla.donghaeguard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final SensorEventRepository sensorEventRepository;
    private final UltrasonicEventRepository ultrasonicEventRepository;
    private final TiltEventRepository tiltEventRepository;
    private final CameraEventRepository cameraEventRepository;

    @Transactional
    public TripStartResponse startTrip(TripStartRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new RuntimeException("디바이스를 찾을 수 없습니다."));

        Trip trip = Trip.builder()
                .user(user)
                .device(device)
                .tripStatus(Trip.TripStatus.STARTED)
                .startedAt(request.getStartedAt())
                .eventCount(0)
                .build();

        Trip saved = tripRepository.save(trip);

        return new TripStartResponse(
                saved.getTripId(),
                saved.getTripStatus().name(),
                saved.getStartedAt()
        );
    }

    @Transactional
    public TripEndResponse endTrip(Long tripId, TripEndRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        int eventCount = sensorEventRepository.findByTripTripId(tripId).size();

        Trip updatedTrip = Trip.builder()
                .tripId(trip.getTripId())
                .user(trip.getUser())
                .device(trip.getDevice())
                .tripStatus(Trip.TripStatus.ENDED)
                .startedAt(trip.getStartedAt())
                .endedAt(request.getEndedAt())
                .eventCount(eventCount)
                .highestRiskLevel(trip.getHighestRiskLevel())
                .build();

        Trip saved = tripRepository.save(updatedTrip);

        return new TripEndResponse(
                saved.getTripId(),
                saved.getTripStatus().name(),
                saved.getEventCount(),
                saved.getEndedAt()
        );
    }

    @Transactional(readOnly = true)
    public TripSummaryResponse getTripSummary(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        return new TripSummaryResponse(
                trip.getStartedAt(),
                trip.getEndedAt(),
                trip.getEventCount(),
                trip.getHighestRiskLevel() != null ? trip.getHighestRiskLevel().name() : null
        );
    }

    @Transactional(readOnly = true)
    public TripStatusResponse getTripStatus(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        return new TripStatusResponse(
                trip.getTripStatus().name(),
                trip.getEventCount()
        );
    }

    @Transactional(readOnly = true)
    public List<TripListResponse> getTripList(Long userId) {
        List<Trip> trips = tripRepository.findByUserUserIdOrderByStartedAtDesc(userId);

        return trips.stream()
                .map(trip -> new TripListResponse(
                        trip.getTripId(),
                        trip.getStartedAt(),
                        trip.getEndedAt(),
                        trip.getTripStatus().name(),
                        trip.getEventCount(),
                        trip.getHighestRiskLevel() != null ? trip.getHighestRiskLevel().name() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTripDetail(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("주행 세션을 찾을 수 없습니다."));

        List<SensorEvent> events = sensorEventRepository.findByTripTripId(tripId);

        List<SensorEventDetailResponse> eventResponses = events.stream()
                .map(this::toSensorEventDetailResponse)
                .toList();

        return new TripDetailResponse(
                trip.getTripId(),
                trip.getStartedAt(),
                trip.getEndedAt(),
                trip.getTripStatus().name(),
                trip.getEventCount(),
                trip.getHighestRiskLevel() != null ? trip.getHighestRiskLevel().name() : null,
                eventResponses
        );
    }

    private SensorEventDetailResponse toSensorEventDetailResponse(SensorEvent e) {
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

        return new SensorEventDetailResponse(
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
}