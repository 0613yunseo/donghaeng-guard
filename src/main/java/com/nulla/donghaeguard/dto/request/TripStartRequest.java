package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class TripStartRequest {
    private Long userId;
    private String deviceId;
    private LocalDateTime startedAt;
}