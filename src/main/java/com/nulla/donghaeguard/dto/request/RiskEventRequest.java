package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class RiskEventRequest {
    private Long tripId;
    private String deviceId;
    private String sensorType;   // TOF, ULTRASONIC, TILT
    private String riskType;     // OBSTACLE, STEP, SLOPE 등
    private String riskLevel;    // SAFE, WARNING, DANGER
    private Integer distanceMm;  // 초음파/TOF 이벤트용
    private Double angleDeg;     // TILT 이벤트용
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;
}