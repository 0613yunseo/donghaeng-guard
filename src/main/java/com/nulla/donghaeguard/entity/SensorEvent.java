package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorEvent {

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
    private SensorType sensorType;  // ULTRASONIC, TILT, CAMERA

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;  // SAFE, WARNING, DANGER

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    public enum SensorType {
        ULTRASONIC, TILT, CAMERA
    }

    public enum RiskLevel {
        SAFE, WARNING, DANGER
    }
}