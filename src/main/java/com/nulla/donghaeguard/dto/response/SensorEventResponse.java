package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SensorEventResponse {
    private Long eventId;
    private String sensorType;
    private String riskLevel;
    private boolean saved;
}