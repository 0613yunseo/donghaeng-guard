package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 0.2초 판정 주기 1회 = 1행.
 * 같은 주기의 초음파/기울기/카메라 sensor_events는 measurement_id로 연결된다.
 * detected_at은 UTC, 밀리초 정밀도.
 */
@Entity
@Table(name = "measurements",
        indexes = {
                @Index(name = "idx_measurements_trip_time", columnList = "trip_id, detected_at"),
                @Index(name = "idx_measurements_device_time", columnList = "device_id, detected_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Measurement {

    @Id
    @Column(name = "measurement_id", length = 64)
    private String measurementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /** 측정 시각 (UTC, 밀리초) */
    @Column(name = "detected_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime detectedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // ===== 판정 결과 =====
    /** 내부 위험 단계 1~5 */
    @Column(name = "risk_stage", nullable = false)
    private Integer riskStage;

    /** 3단계 위험 등급 (SAFE / WARNING / DANGER) */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private SensorEvent.RiskLevel riskLevel;

    /** 해당 주기에 참이 된 모든 규칙 코드 (쉼표 구분) */
    @Column(name = "matched_rules", length = 500)
    private String matchedRules;

    /** 판정 로직 버전 */
    @Column(name = "rule_version", length = 32)
    private String ruleVersion;

    /** 최종 단계를 만든 규칙 기반 판정 이유 */
    @Column(name = "reason", length = 255)
    private String reason;

    // ===== 센서 유효성 =====
    @Column(name = "board1_valid")
    private Boolean board1Valid;

    @Column(name = "board2_valid")
    private Boolean board2Valid;

    @Column(name = "front_us_valid")
    private Boolean frontUsValid;

    @Column(name = "rear_us_valid")
    private Boolean rearUsValid;

    @Column(name = "tilt_valid")
    private Boolean tiltValid;

    @Column(name = "camera_valid")
    private Boolean cameraValid;

    // ===== 초음파 요약 =====
    @Column(name = "front_us_min_mm")
    private Integer frontUsMinMm;

    @Column(name = "rear_us_min_mm")
    private Integer rearUsMinMm;

    // ===== 기울기 =====
    /** MPU6050 원본 각도 */
    @Column(name = "pitch_raw")
    private Double pitchRaw;

    /** 평지 기준(-82°) 대비 변화량 = |82 - |raw|| */
    @Column(name = "pitch_diff")
    private Double pitchDiff;

    // ===== 카메라 분석 (객체 미검출 주기에도 저장) =====
    @Column(name = "object_detected")
    private Boolean objectDetected;

    @Column(name = "detected_object", length = 64)
    private String detectedObject;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "max_box_ratio")
    private Double maxBoxRatio;

    @Column(name = "std_dev")
    private Double stdDev;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}
