package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.request.RiskEventRequest;
import com.nulla.donghaeguard.dto.response.RiskEventResponse;
import com.nulla.donghaeguard.service.RiskEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-events")
@RequiredArgsConstructor
public class RiskEventController {

    private final RiskEventService riskEventService;

    @PostMapping
    public ApiResponse<RiskEventResponse> saveRiskEvent(@RequestBody RiskEventRequest request) {
        RiskEventResponse response = riskEventService.saveRiskEvent(request);
        return ApiResponse.ok(response);
    }
}