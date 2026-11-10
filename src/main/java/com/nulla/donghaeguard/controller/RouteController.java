package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.dto.request.RouteRequest;
import com.nulla.donghaeguard.dto.response.RouteResponse;
import com.nulla.donghaeguard.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * 안전 경로 추천
     * POST /api/routes/recommend
     *
     * 요청: { startLat, startLng, endLat, endLng }
     * 응답: 경로 좌표 + 위험 구간 + 안전 점수
     */
    @PostMapping("/recommend")
    public RouteResponse recommendRoute(@RequestBody RouteRequest request) {
        return routeService.getRecommendedRoute(request);
    }
}
