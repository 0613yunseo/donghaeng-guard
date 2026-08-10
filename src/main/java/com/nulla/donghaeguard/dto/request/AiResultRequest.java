package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.util.List;

@Getter
public class AiResultRequest {
    private List<ZoneRequest> zones;

    @Getter
    public static class ZoneRequest {
        private Double latitude;
        private Double longitude;
        private Integer riskScore;
        private String riskGrade;
        private Integer zoneEventCount;
        private String reason;
    }
}