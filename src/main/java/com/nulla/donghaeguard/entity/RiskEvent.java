package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensorType sensorType;

    private String riskType;  // OBSTACLE, STEP 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private Integer distanceMm;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    public enum SensorType {
        TOF, ULTRASONIC
    }

    public enum RiskLevel {
        SAFE, WARNING, DANGER
    }
}