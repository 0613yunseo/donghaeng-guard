package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.request.TripEndRequest;
import com.nulla.donghaeguard.dto.request.TripStartRequest;
import com.nulla.donghaeguard.dto.response.*;
import com.nulla.donghaeguard.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping("/start")
    public ApiResponse<TripStartResponse> startTrip(@RequestBody TripStartRequest request) {
        TripStartResponse response = tripService.startTrip(request);
        return ApiResponse.ok(response);
    }

    @PatchMapping("/{tripId}/end")
    public ApiResponse<TripEndResponse> endTrip(
            @PathVariable Long tripId,
            @RequestBody TripEndRequest request) {
        TripEndResponse response = tripService.endTrip(tripId, request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{tripId}/summary")
    public ApiResponse<TripSummaryResponse> getTripSummary(@PathVariable Long tripId) {
        TripSummaryResponse response = tripService.getTripSummary(tripId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{tripId}/status")
    public ApiResponse<TripStatusResponse> getTripStatus(@PathVariable Long tripId) {
        TripStatusResponse response = tripService.getTripStatus(tripId);
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<List<TripListResponse>> getTripList(@RequestParam Long userId) {
        List<TripListResponse> response = tripService.getTripList(userId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripDetailResponse> getTripDetail(@PathVariable Long tripId) {
        TripDetailResponse response = tripService.getTripDetail(tripId);
        return ApiResponse.ok(response);
    }
}