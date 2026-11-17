package com.nulla.donghaeguard.dto.request;

import lombok.Getter;

@Getter
public class RouteRequest {
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
}
