package com.nulla.donghaeguard.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/**
 * 센서 PC → 백엔드: 0.2초 판정 주기 1회분 통합 데이터
 * detectedAt은 UTC ISO-8601 (예: 2026-10-13T06:21:55.123Z)
 */
public record MeasurementRequest(
        @NotBlank String measurementId,
        @NotNull Long tripId,
        @NotBlank String deviceId,
        @NotNull Instant detectedAt,
        Double latitude,
        Double longitude,

        @NotNull @Min(1) @Max(5) Integer riskStage,
        @NotBlank String riskLevel,
        List<String> matchedRules,
        String ruleVersion,
        String reason,

        Boolean board1Valid,
        Boolean board2Valid,

        @Valid Ultrasonic ultrasonic,
        @Valid Tilt tilt,
        @Valid Camera camera
) {
    public record Ultrasonic(
            Boolean frontValid,
            Boolean rearValid,
            Integer frontMinMm,
            Integer rearMinMm,
            List<@Valid Reading> readings
    ) {}

    public record Reading(
            @NotNull @Min(1) @Max(5) Integer sensorId,
            @NotNull Integer distanceMm
    ) {}

    public record Tilt(
            Boolean valid,
            Double pitchRaw,
            Double pitchDiff,
            String slopeStatus
    ) {}

    public record Camera(
            Boolean valid,
            Boolean objectDetected,
            String detectedObject,
            Double confidence,
            Double maxBoxRatio,
            Double stdDev,
            String imageUrl
    ) {}
}
