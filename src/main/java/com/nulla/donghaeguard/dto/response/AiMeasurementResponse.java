package com.nulla.donghaeguard.dto.response;

import java.util.List;

/**
 * AI 조회용: 측정 주기 1회 = 1행 (프레임 단위)
 * detectedAt은 UTC, 밀리초 포함 문자열 (yyyy-MM-dd'T'HH:mm:ss.SSS'Z')
 */
public record AiMeasurementResponse(
        String measurementId,
        Long tripId,
        String deviceId,
        String detectedAt,

        Integer riskStage,
        String riskLevel,
        List<String> matchedRules,
        String ruleVersion,
        String reason,

        Double latitude,
        Double longitude,

        Boolean board1Valid,
        Boolean board2Valid,
        Boolean frontUsValid,
        Boolean rearUsValid,
        Integer frontUsMinMm,
        Integer rearUsMinMm,
        Integer us1Mm,
        Integer us2Mm,
        Integer us3Mm,
        Integer us4Mm,
        Integer us5Mm,

        Boolean tiltValid,
        Double pitchRaw,
        Double pitchDiff,
        String slopeStatus,

        Boolean cameraValid,
        Boolean objectDetected,
        String detectedObject,
        Double confidence,
        Double maxBoxRatio,
        Double stdDev,
        String imageUrl
) {}
