package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class TripEndRequest {
    private LocalDateTime endedAt;
    private Integer pendingEventCount;
}