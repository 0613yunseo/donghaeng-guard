package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.RouteRequest;
import com.nulla.donghaeguard.dto.response.RouteResponse;
import com.nulla.donghaeguard.entity.RiskZone;
import com.nulla.donghaeguard.repository.RiskZoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class RouteService {

    private final RestTemplate restTemplate;
    private final RiskZoneRepository riskZoneRepository;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final double RISK_CHECK_RADIUS_M = 50.0;
    private static final double BBOX_MARGIN = 0.005;

    public RouteService(RestTemplate restTemplate, RiskZoneRepository riskZoneRepository) {
        this.restTemplate = restTemplate;
        this.riskZoneRepository = riskZoneRepository;
    }

    public RouteResponse getRecommendedRoute(RouteRequest req) {
        // 1. 카카오 길찾기 API 호출
        KakaoRouteResult kakaoResult = fetchKakaoRoute(req);

        List<RouteResponse.RoutePoint> path = kakaoResult.path;
        int totalDistance = kakaoResult.distance;
        int totalDuration = kakaoResult.duration;

        if (path.isEmpty()) {
            path = createStraightPath(req);
            totalDistance = (int) calculateDistance(
                    req.getStartLat(), req.getStartLng(),
                    req.getEndLat(), req.getEndLng());
            totalDuration = totalDistance;
        }

        // 2. 경로 바운딩 박스 내 risk_zones 조회
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
        for (RouteResponse.RoutePoint p : path) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLng = Math.min(minLng, p.getLongitude());
            maxLng = Math.max(maxLng, p.getLongitude());
        }

        List<RiskZone> nearbyZones = riskZoneRepository.findInBoundingBox(
                minLat - BBOX_MARGIN, maxLat + BBOX_MARGIN,
                minLng - BBOX_MARGIN, maxLng + BBOX_MARGIN
        );

        // 3. 경로에서 50m 이내 위험 구간 필터링
        List<RouteResponse.RiskZoneInfo> riskZonesOnRoute = new ArrayList<>();
        for (RiskZone zone : nearbyZones) {
            double minDist = Double.MAX_VALUE;
            for (RouteResponse.RoutePoint p : path) {
                double dist = calculateDistance(
                        p.getLatitude(), p.getLongitude(),
                        zone.getLatitude(), zone.getLongitude());
                minDist = Math.min(minDist, dist);
            }

            if (minDist <= RISK_CHECK_RADIUS_M) {
                riskZonesOnRoute.add(RouteResponse.RiskZoneInfo.builder()
                        .zoneId(zone.getZoneId())
                        .latitude(zone.getLatitude())
                        .longitude(zone.getLongitude())
                        .riskScore(zone.getRiskScore())
                        .riskGrade(zone.getRiskGrade().name())
                        .reason(zone.getReason())
                        .distanceFromRoute(Math.round(minDist * 10) / 10.0)
                        .build());
            }
        }

        // 4. 안전 점수 계산
        double safetyScore = calculateSafetyScore(riskZonesOnRoute);

        return RouteResponse.builder()
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .riskZoneCount(riskZonesOnRoute.size())
                .safetyScore(safetyScore)
                .path(path)
                .riskZones(riskZonesOnRoute)
                .build();
    }

    /** 카카오 API 응답을 담는 내부 클래스 */
    private static class KakaoRouteResult {
        List<RouteResponse.RoutePoint> path = new ArrayList<>();
        int distance = 0;
        int duration = 0;
    }

    @SuppressWarnings("unchecked")
    private KakaoRouteResult fetchKakaoRoute(RouteRequest req) {
        KakaoRouteResult result = new KakaoRouteResult();

        try {
            String url = String.format(
                    "https://apis-navi.kakaomobility.com/v1/directions?origin=%f,%f&destination=%f,%f&priority=RECOMMEND",
                    req.getStartLng(), req.getStartLat(),
                    req.getEndLng(), req.getEndLat()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");

                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);

                    // 거리/시간 파싱
                    Map<String, Object> summary = (Map<String, Object>) route.get("summary");
                    if (summary != null) {
                        Number dist = (Number) summary.get("distance");
                        Number dur = (Number) summary.get("duration");
                        if (dist != null) result.distance = dist.intValue();
                        if (dur != null) result.duration = dur.intValue();
                    }

                    // 경로 좌표 파싱
                    List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
                    if (sections != null) {
                        for (Map<String, Object> section : sections) {
                            List<Map<String, Object>> roads = (List<Map<String, Object>>) section.get("roads");
                            if (roads != null) {
                                for (Map<String, Object> road : roads) {
                                    List<Number> vertexes = (List<Number>) road.get("vertexes");
                                    if (vertexes != null) {
                                        for (int i = 0; i < vertexes.size() - 1; i += 2) {
                                            double lng = vertexes.get(i).doubleValue();
                                            double lat = vertexes.get(i + 1).doubleValue();
                                            result.path.add(new RouteResponse.RoutePoint(lat, lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[경로] 카카오 API 호출 실패: {}", e.getMessage());
        }

        return result;
    }

    private List<RouteResponse.RoutePoint> createStraightPath(RouteRequest req) {
        List<RouteResponse.RoutePoint> path = new ArrayList<>();
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double lat = req.getStartLat() + (req.getEndLat() - req.getStartLat()) * ratio;
            double lng = req.getStartLng() + (req.getEndLng() - req.getStartLng()) * ratio;
            path.add(new RouteResponse.RoutePoint(lat, lng));
        }
        return path;
    }

    private double calculateSafetyScore(List<RouteResponse.RiskZoneInfo> riskZones) {
        double score = 100.0;
        for (RouteResponse.RiskZoneInfo zone : riskZones) {
            switch (zone.getRiskGrade()) {
                case "HIGH" -> score -= 20;
                case "MEDIUM" -> score -= 10;
                case "LOW" -> score -= 5;
            }
        }
        return Math.max(0, score);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}