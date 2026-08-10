package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripStatusResponse {
    private String tripStatus;
    private Integer eventCount;
}