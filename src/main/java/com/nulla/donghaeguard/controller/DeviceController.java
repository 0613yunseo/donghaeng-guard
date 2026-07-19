package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.request.DeviceRequest;
import com.nulla.donghaeguard.dto.response.DeviceResponse;
import com.nulla.donghaeguard.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ApiResponse<DeviceResponse> registerDevice(@RequestBody DeviceRequest request) {
        DeviceResponse response = deviceService.registerDevice(request);
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<List<DeviceResponse>> getDevices(@RequestParam Long userId) {
        List<DeviceResponse> response = deviceService.getDevices(userId);
        return ApiResponse.ok(response);
    }
}