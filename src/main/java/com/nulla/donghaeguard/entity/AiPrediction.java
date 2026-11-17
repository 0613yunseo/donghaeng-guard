package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_predictions",
        indexes = @Index(name = "idx_ai_pred_measurement", columnList = "measurement_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "measurement_id", length = 64, nullable = false)
    private String measurementId;

    @Column(name = "trip_id")
    private Long tripId;

    /** AI 모델 타입: TILT, CAMERA */
    @Column(name = "model_type", length = 32, nullable = false)
    private String modelType;

    /** AI 모델 버전 (예: tilt-rf-third-v1) */
    @Column(name = "model_version", length = 64)
    private String modelVersion;

    /** AI 예측 위험 등급 */
    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_risk", length = 16)
    private SensorEvent.RiskLevel predictedRisk;

    /** AI 예측 확률 (0.0 ~ 1.0, 보정되지 않은 클래스 점수) */
    @Column(name = "predicted_probability")
    private Double predictedProbability;

    /** 기존 규칙 기반 위험 등급 (비교용) */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_risk", length = 16)
    private SensorEvent.RiskLevel ruleRisk;

    /**
     * AI 호출 상태:
     * SUCCESS, TIMEOUT, VALIDATION_ERROR (422),
     * MODEL_NOT_FOUND (404), SERVER_ERROR (5xx), ERROR
     */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** AI 서버가 반환한 경고 메시지 (쉼표 구분) */
    @Column(name = "warnings", length = 500)
    private String warnings;

    @Column(name = "predicted_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime predictedAt;
}
