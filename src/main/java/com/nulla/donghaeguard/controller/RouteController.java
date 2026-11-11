package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.dto.request.RouteRequest;
import com.nulla.donghaeguard.dto.response.RouteResponse;
import com.nulla.donghaeguard.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendRoute(@RequestBody RouteRequest request) {
        if (request.getStartLat() == null || request.getStartLng() == null
                || request.getEndLat() == null || request.getEndLng() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "출발지(startLat, startLng)와 목적지(endLat, endLng)를 모두 입력해주세요."
            ));
        }

        if (!isValidKoreaCoord(request.getStartLat(), request.getStartLng())
                || !isValidKoreaCoord(request.getEndLat(), request.getEndLng())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_COORDINATES",
                    "message", "좌표가 유효하지 않습니다. 한국 영역(위도 33~39, 경도 124~132)을 확인해주세요."
            ));
        }

        if (Math.abs(request.getStartLat() - request.getEndLat()) < 0.0001
                && Math.abs(request.getStartLng() - request.getEndLng()) < 0.0001) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SAME_LOCATION",
                    "message", "출발지와 목적지가 동일합니다."
            ));
        }

        try {
            RouteResponse response = routeService.getRecommendedRoute(request);
            return ResponseEntity.ok(response);
        } catch (RouteService.KakaoApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "KAKAO_API_ERROR",
                    "message", "카카오 길찾기 API 호출에 실패했습니다: " + e.getMessage()
            ));
        } catch (RouteService.RouteNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "ROUTE_NOT_FOUND",
                    "message", "출발지에서 목적지까지의 경로를 찾을 수 없습니다."
            ));
        }
    }

    private boolean isValidKoreaCoord(double lat, double lng) {
        return lat >= 33.0 && lat <= 39.0 && lng >= 124.0 && lng <= 132.0;
    }
}
