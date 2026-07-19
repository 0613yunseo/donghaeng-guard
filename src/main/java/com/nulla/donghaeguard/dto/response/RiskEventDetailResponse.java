package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RiskEventDetailResponse {
    private Long eventId;
    private String riskLevel;
    private Integer distanceMm;
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;
}