package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class RouteResponse {

    private int totalDistance;
    private int totalDuration;
    private int riskZoneCount;
    private double safetyScore;
    private boolean fallback;
    private List<RoutePoint> path;
    private List<RiskZoneInfo> riskZones;

    @Getter
    @AllArgsConstructor
    public static class RoutePoint {
        private double latitude;
        private double longitude;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RiskZoneInfo {
        private Long zoneId;
        private double latitude;
        private double longitude;
        private int riskScore;
        private String riskGrade;
        private String reason;
        private double distanceFromRoute;
    }
}
