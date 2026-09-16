package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class SensorEventRequest {
    private Long tripId;
    private String deviceId;
    private String sensorType;  // ULTRASONIC, TILT, CAMERA
    private String riskLevel;   // SAFE, WARNING, DANGER
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;

    // 초음파 센서용
    private Integer sensorId;
    private Integer distanceMm;

    // 기울기 센서용
    private Float pitch;
    private String slopeStatus;  // NORMAL, WARNING, DANGER

    // 카메라용
    private String detectedObject;
    private Float confidence;
    private Float stdDev;
    private String imageUrl;
}