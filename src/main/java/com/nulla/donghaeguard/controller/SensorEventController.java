package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.request.SensorEventRequest;
import com.nulla.donghaeguard.dto.response.SensorEventResponse;
import com.nulla.donghaeguard.service.SensorEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensor-events")
@RequiredArgsConstructor
public class SensorEventController {

    private final SensorEventService sensorEventService;

    @PostMapping
    public ApiResponse<SensorEventResponse> saveSensorEvent(
            @RequestBody SensorEventRequest request) {
        SensorEventResponse response = sensorEventService.saveSensorEvent(request);
        return ApiResponse.ok(response);
    }
}