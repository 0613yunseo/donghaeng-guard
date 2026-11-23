package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus tripStatus;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(columnDefinition = "int default 0")
    private Integer eventCount;

    @Enumerated(EnumType.STRING)
    private HighestRiskLevel highestRiskLevel;

    public enum TripStatus {
        STARTED, ENDED
    }

    // 순서 중요: ordinal() 비교에 사용 (SAFE=0 < WARNING=1 < DANGER=2)
    public enum HighestRiskLevel {
        SAFE, WARNING, DANGER
    }

    private String videoUrl;

    public void updateVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void updateHighestRiskLevel(HighestRiskLevel level) {
        this.highestRiskLevel = level;
    }

    public void incrementEventCount() {
        this.eventCount = (this.eventCount != null ? this.eventCount : 0) + 1;
    }
}