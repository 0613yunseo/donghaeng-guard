package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String deviceName;

    private String sensorTypes;  // "TOF,ULTRASONIC"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus deviceStatus;

    private LocalDateTime lastConnectedAt;

    private LocalDateTime createdAt;

    public enum DeviceStatus {
        CONNECTED, DISCONNECTED, ERROR
    }
}