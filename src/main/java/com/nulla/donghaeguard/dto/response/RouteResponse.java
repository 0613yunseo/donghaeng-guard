package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class RouteResponse {

    private int totalDistance;       // 총 거리 (m)
    private int totalDuration;      // 총 소요 시간 (초)
    private int riskZoneCount;      // 경로 위 위험 구간 수
    private double safetyScore;     // 안전 점수 (0~100, 높을수록 안전)
    private List<RoutePoint> path;  // 경로 좌표 리스트
    private List<RiskZoneInfo> riskZones; // 경로 위 위험 구간 정보

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
        private double distanceFromRoute; // 경로에서의 거리 (m)
    }
}
