package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ultrasonic_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UltrasonicEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SensorEvent sensorEvent;

    @Column(nullable = false)
    private Integer sensorId;  // 1~5 (어떤 초음파 센서인지)

    @Column(nullable = false)
    private Integer distanceMm;  // 감지 거리 (mm)
}