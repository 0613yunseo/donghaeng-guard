package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tilt_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TiltEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SensorEvent sensorEvent;

    @Column(nullable = false)
    private Float pitch;  // 기울기 각도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlopeStatus slopeStatus;  // NORMAL, WARNING, DANGER

    public enum SlopeStatus {
        NORMAL, WARNING, DANGER
    }
}