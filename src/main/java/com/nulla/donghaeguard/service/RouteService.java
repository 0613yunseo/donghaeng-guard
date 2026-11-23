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

    @Value("${tmap.api.key}")
    private String tmapApiKey;

    private static final String TMAP_PEDESTRIAN_URL =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1";

    private static final double RISK_CHECK_RADIUS_M = 50.0;
    private static final double BBOX_MARGIN = 0.005;

    public RouteService(RestTemplate restTemplate, RiskZoneRepository riskZoneRepository) {
        this.restTemplate = restTemplate;
        this.riskZoneRepository = riskZoneRepository;
    }

    public static class TmapApiException extends RuntimeException {
        public TmapApiException(String message) { super(message); }
    }

    public static class RouteNotFoundException extends RuntimeException {
        public RouteNotFoundException(String message) { super(message); }
    }

    public RouteResponse getRecommendedRoute(RouteRequest req) {
        TmapRouteResult tmapResult = fetchTmapPedestrianRoute(req);

        List<RouteResponse.RoutePoint> path = tmapResult.path;
        int totalDistance = tmapResult.distance;
        int totalDuration = tmapResult.duration;

        if (path.isEmpty()) {
            if (tmapResult.apiError) {
                throw new TmapApiException(tmapResult.errorMessage);
            }
            throw new RouteNotFoundException("경로를 찾을 수 없습니다.");
        }

        // 경로 주변 위험구역 조회 (bounding box)
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

        // 경로 포인트와 위험구역 거리 계산 (50m 이내)
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

        double safetyScore = calculateSafetyScore(riskZonesOnRoute);

        return RouteResponse.builder()
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .riskZoneCount(riskZonesOnRoute.size())
                .safetyScore(safetyScore)
                .fallback(false)
                .path(path)
                .riskZones(riskZonesOnRoute)
                .build();
    }

    // ── Tmap 보행자 길찾기 API 호출 ──

    private static class TmapRouteResult {
        List<RouteResponse.RoutePoint> path = new ArrayList<>();
        int distance = 0;  // 미터
        int duration = 0;  // 초
        boolean apiError = false;
        String errorMessage = "";
    }

    @SuppressWarnings("unchecked")
    private TmapRouteResult fetchTmapPedestrianRoute(RouteRequest req) {
        TmapRouteResult result = new TmapRouteResult();

        try {
            // 요청 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("appKey", tmapApiKey);

            // 요청 바디
            Map<String, String> body = new HashMap<>();
            body.put("startX", String.valueOf(req.getStartLng()));  // 경도
            body.put("startY", String.valueOf(req.getStartLat()));  // 위도
            body.put("endX", String.valueOf(req.getEndLng()));
            body.put("endY", String.valueOf(req.getEndLat()));
            body.put("startName", URLEncodeKorean("출발지"));
            body.put("endName", URLEncodeKorean("도착지"));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            // POST 요청
            ResponseEntity<Map> response = restTemplate.exchange(
                    TMAP_PEDESTRIAN_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // GeoJSON FeatureCollection 파싱
                List<Map<String, Object>> features =
                        (List<Map<String, Object>>) responseBody.get("features");

                if (features == null || features.isEmpty()) {
                    return result;
                }

                for (Map<String, Object> feature : features) {
                    Map<String, Object> geometry =
                            (Map<String, Object>) feature.get("geometry");
                    Map<String, Object> properties =
                            (Map<String, Object>) feature.get("properties");

                    if (geometry == null) continue;

                    String type = (String) geometry.get("type");

                    // 첫 번째 Feature(Point)에 totalDistance, totalTime 존재
                    if (properties != null && properties.containsKey("totalDistance")) {
                        Number dist = (Number) properties.get("totalDistance");
                        Number time = (Number) properties.get("totalTime");
                        if (dist != null) result.distance = dist.intValue();
                        if (time != null) result.duration = time.intValue();
                    }

                    // LineString → 경로 좌표 추출
                    if ("LineString".equals(type)) {
                        List<List<Number>> coordinates =
                                (List<List<Number>>) geometry.get("coordinates");
                        if (coordinates != null) {
                            for (List<Number> coord : coordinates) {
                                double lng = coord.get(0).doubleValue();
                                double lat = coord.get(1).doubleValue();
                                result.path.add(new RouteResponse.RoutePoint(lat, lng));
                            }
                        }
                    }
                    // Point → 경유지 (경로 포인트에 추가)
                    else if ("Point".equals(type)) {
                        List<Number> coordinates =
                                (List<Number>) geometry.get("coordinates");
                        if (coordinates != null && coordinates.size() >= 2) {
                            double lng = coordinates.get(0).doubleValue();
                            double lat = coordinates.get(1).doubleValue();
                            result.path.add(new RouteResponse.RoutePoint(lat, lng));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[경로] Tmap 보행자 길찾기 API 호출 실패: {}", e.getMessage());
            result.apiError = true;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    private String URLEncodeKorean(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }

    // ── 안전 점수 계산 ──

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

    // ── Haversine 거리 계산 ──

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