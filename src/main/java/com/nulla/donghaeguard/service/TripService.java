package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.TripEndRequest;
import com.nulla.donghaeguard.dto.request.TripStartRequest;
import com.nulla.donghaeguard.dto.response.*;
import com.nulla.donghaeguard.entity.Device;
import com.nulla.donghaeguard.entity.RiskEvent;
import com.nulla.donghaeguard.entity.Trip;
import com.nulla.donghaeguard.entity.User;
import com.nulla.donghaeguard.repository.DeviceRepository;
import com.nulla.donghaeguard.repository.RiskEventRepository;
import com.nulla.donghaeguard.repository.TripRepository;
import com.nulla.donghaeguard.repository.UserRepository;
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
    private final RiskEventRepository riskEventRepository;

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

        int eventCount = riskEventRepository.findByTripTripId(tripId).size();

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

        List<RiskEvent> events = riskEventRepository.findByTripTripId(tripId);

        List<RiskEventDetailResponse> eventResponses = events.stream()
                .map(e -> new RiskEventDetailResponse(
                        e.getEventId(),
                        e.getRiskLevel().name(),
                        e.getDistanceMm(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getDetectedAt()
                ))
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
}

