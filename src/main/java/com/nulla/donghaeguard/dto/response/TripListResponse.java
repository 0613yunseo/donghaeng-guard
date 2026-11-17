package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TripListResponse {
    private Long tripId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private Integer eventCount;
    private String highestRiskLevel;
}