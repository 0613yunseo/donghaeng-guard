package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RiskEventResponse {
    private Long eventId;
    private boolean saved;
}