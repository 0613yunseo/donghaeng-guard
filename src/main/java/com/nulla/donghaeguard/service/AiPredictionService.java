package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.entity.AiPrediction;
import com.nulla.donghaeguard.entity.SensorEvent;
import com.nulla.donghaeguard.repository.AiPredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPredictionService {

    private final AiPredictionRepository aiPredictionRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    // 비동기 실행 제한: 최대 2개 동시, 큐 10개 초과 시 가장 오래된 것 버림
    private final ExecutorService aiExecutor = new ThreadPoolExecutor(
            2, 2, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    /**
     * 트랜잭션 커밋 후 비동기 AI 예측 요청
     */
    public void predictAsync(String measurementId, Long tripId,
                              String ruleRiskLevel,
                              Map<String, Object> tiltData,
                              Map<String, Object> cameraData) {
        aiExecutor.submit(() -> {
            if (tiltData != null && !tiltData.isEmpty()) {
                callPredict(measurementId, tripId, "TILT", ruleRiskLevel, tiltData);
            }
            if (cameraData != null && !cameraData.isEmpty()) {
                callPredict(measurementId, tripId, "CAMERA", ruleRiskLevel, cameraData);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void callPredict(String measurementId, Long tripId,
                              String modelType, String ruleRiskLevel,
                              Map<String, Object> features) {
        AiPrediction.AiPredictionBuilder builder = AiPrediction.builder()
                .measurementId(measurementId)
                .tripId(tripId)
                .modelType(modelType)
                .ruleRisk(SensorEvent.RiskLevel.valueOf(ruleRiskLevel))
                .predictedAt(LocalDateTime.now());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model_type", modelType.toLowerCase(),
                    "features", features
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServerUrl + "/predict",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String prediction = (String) body.get("prediction");
                Number probability = (Number) body.get("probability");
                String modelVersion = (String) body.get("modelVersion");

                // warnings 배열 → 쉼표 구분 문자열
                String warningsStr = null;
                Object warningsObj = body.get("warnings");
                if (warningsObj instanceof List<?> warningsList && !warningsList.isEmpty()) {
                    warningsStr = String.join(",", warningsList.stream()
                            .map(Object::toString).toList());
                }

                builder.predictedRisk(SensorEvent.RiskLevel.valueOf(prediction))
                        .predictedProbability(probability != null ? probability.doubleValue() : null)
                        .modelVersion(modelVersion)
                        .warnings(warningsStr)
                        .status("SUCCESS");
            } else {
                builder.status("ERROR")
                        .errorMessage("HTTP " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[AI] {} 서버 연결 불가: {}", modelType, e.getMessage());
            builder.status("TIMEOUT")
                    .errorMessage(truncate(e.getMessage(), 500));

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 422) {
                log.warn("[AI] {} 입력 검증 실패 (422): {}", modelType, e.getResponseBodyAsString());
                builder.status("VALIDATION_ERROR")
                        .errorMessage(truncate("422: " + e.getResponseBodyAsString(), 500));
            } else if (code == 404) {
                log.warn("[AI] {} 모델 미지원 (404)", modelType);
                builder.status("MODEL_NOT_FOUND")
                        .errorMessage("Model not found: " + modelType);
            } else {
                log.warn("[AI] {} HTTP {}: {}", modelType, code, e.getMessage());
                builder.status("ERROR")
                        .errorMessage(truncate("HTTP " + code + ": " + e.getMessage(), 500));
            }

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("[AI] {} 서버 에러 ({}): {}", modelType, e.getStatusCode(), e.getMessage());
            builder.status("SERVER_ERROR")
                    .errorMessage(truncate(e.getStatusCode() + ": " + e.getResponseBodyAsString(), 500));

        } catch (Exception e) {
            log.error("[AI] {} 예측 에러: {}", modelType, e.getMessage());
            builder.status("ERROR")
                    .errorMessage(truncate(e.getMessage(), 500));
        }

        try {
            aiPredictionRepository.save(builder.build());
        } catch (Exception e) {
            log.error("[AI] 예측 저장 실패: {}", e.getMessage());
        }
    }

    public List<AiPrediction> findByTripId(Long tripId) {
        return aiPredictionRepository.findByTripIdOrderByPredictedAtDesc(tripId);
    }

    public List<AiPrediction> findByMeasurementId(String measurementId) {
        return aiPredictionRepository.findByMeasurementId(measurementId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "Unknown error";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
