package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.response.RiskZoneResponse;
import com.nulla.donghaeguard.service.RiskZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-zones")
@RequiredArgsConstructor
public class RiskZoneController {

    private final RiskZoneService riskZoneService;

    @GetMapping
    public ApiResponse<List<RiskZoneResponse>> getRiskZones() {
        List<RiskZoneResponse> response = riskZoneService.getRiskZones();
        return ApiResponse.ok(response);
    }
}