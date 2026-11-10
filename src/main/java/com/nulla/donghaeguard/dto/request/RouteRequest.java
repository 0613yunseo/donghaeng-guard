package com.nulla.donghaeguard.dto.request;

import lombok.Getter;

@Getter
public class RouteRequest {
    private Double startLat;    // 출발지 위도
    private Double startLng;    // 출발지 경도
    private Double endLat;      // 목적지 위도
    private Double endLng;      // 목적지 경도
}
