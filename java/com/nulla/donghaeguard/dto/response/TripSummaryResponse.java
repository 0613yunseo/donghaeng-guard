package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TripSummaryResponse {
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer eventCount;
    private String highestRiskLevel;
}