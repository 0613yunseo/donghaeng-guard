package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TripStartResponse {
    private Long tripId;
    private String status;
    private LocalDateTime startedAt;
}