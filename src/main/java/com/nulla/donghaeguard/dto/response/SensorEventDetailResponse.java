package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SensorEventDetailResponse {
    private Long eventId;
    private String sensorType;
    private String riskLevel;
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;

    // 초음파용
    private Integer sensorId;
    private Integer distanceMm;

    // 기울기용
    private Float pitch;
    private String slopeStatus;

    // 카메라용
    private String detectedObject;
    private Float confidence;
    private Float stdDev;
    private String imageUrl;
}