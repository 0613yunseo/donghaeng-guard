package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class RiskEventRequest {
    private Long tripId;
    private String deviceId;
    private String sensorType;   // TOF, ULTRASONIC
    private String riskType;     // OBSTACLE, STEP 등
    private String riskLevel;    // SAFE, WARNING, DANGER
    private Integer distanceMm;
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;
}