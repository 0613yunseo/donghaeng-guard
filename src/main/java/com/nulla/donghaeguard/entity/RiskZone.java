package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_zones")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long zoneId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer riskScore;  // 0~100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskGrade riskGrade;

    @Column(columnDefinition = "int default 0")
    private Integer zoneEventCount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum RiskGrade {
        LOW, MEDIUM, HIGH
    }
}