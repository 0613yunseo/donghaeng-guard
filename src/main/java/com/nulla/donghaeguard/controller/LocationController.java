package com.nulla.donghaeguard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    // tripId별 최신 GPS 저장 (메모리)
    private static final ConcurrentHashMap<Long, double[]> latestLocation = new ConcurrentHashMap<>();

    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> updateLocation(
            @RequestBody Map<String, Object> body) {
        Long tripId = Long.valueOf(body.get("tripId").toString());
        double latitude = Double.parseDouble(body.get("latitude").toString());
        double longitude = Double.parseDouble(body.get("longitude").toString());

        latestLocation.put(tripId, new double[]{latitude, longitude});
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    public static double[] getLatestLocation(Long tripId) {
        return latestLocation.getOrDefault(tripId, new double[]{0.0, 0.0});
    }
}