package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_explanations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_explain_mid",
                columnNames = {"measurement_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "measurement_id", length = 64, nullable = false)
    private String measurementId;

    @Column(name = "trip_id")
    private Long tripId;

    /** 규칙 기반 위험 등급 */
    @Column(name = "rule_risk", length = 16)
    private String ruleRisk;

    /** LLM 생성 한국어 설명 */
    @Column(name = "explanation", length = 2000)
    private String explanation;

    /** 설명 생성 출처 (예: groq-llama3) */
    @Column(name = "source", length = 64)
    private String source;

    /** 안전 제어 주체 (RULE_BASED) */
    @Column(name = "safety_control_authority", length = 32)
    private String safetyControlAuthority;

    /**
     * 상태: EXPLAINED, TIMEOUT, VALIDATION_ERROR, SERVER_ERROR, ERROR
     */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;
}
