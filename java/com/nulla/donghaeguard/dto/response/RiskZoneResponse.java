package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RiskZoneResponse {
    private Long zoneId;
    private Double latitude;
    private Double longitude;
    private Integer riskScore;
    private String riskGrade;
    private Integer zoneEventCount;
    private String reason;
    private LocalDateTime updatedAt;
}