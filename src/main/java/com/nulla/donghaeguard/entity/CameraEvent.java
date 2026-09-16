package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "camera_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SensorEvent sensorEvent;

    private String detectedObject;  // 감지된 객체 (person, chair 등)

    private Float confidence;  // 신뢰도 (0.0 ~ 1.0)

    private Float stdDev;  // 노면 표준편차 (MiDaS Depth)

    private String imageUrl;  // 위험 감지 시 캡처 이미지 경로
}