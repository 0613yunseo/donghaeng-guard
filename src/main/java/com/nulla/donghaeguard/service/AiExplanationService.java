package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.entity.AiExplanation;
import com.nulla.donghaeguard.entity.AiPrediction;
import com.nulla.donghaeguard.repository.AiExplanationRepository;
import com.nulla.donghaeguard.repository.AiPredictionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class AiExplanationService {

    private final AiExplanationRepository aiExplanationRepository;
    private final AiPredictionRepository aiPredictionRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    // trip별 마지막 /explain 호출 시각 (30초 쓰로틀링)
    private final ConcurrentHashMap<Long, Long> lastExplainTime = new ConcurrentHashMap<>();
    private static final long EXPLAIN_INTERVAL_MS = 30_000; // 30초

    private final ExecutorService explainExecutor = new ThreadPoolExecutor(
            1, 1, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5),
            (r, executor) -> log.warn("[AI Explain] 큐 초과 — 요청 버림")
    );

    public AiExplanationService(AiExplanationRepository aiExplanationRepository,
                                AiPredictionRepository aiPredictionRepository,
                                RestTemplate restTemplate) {
        this.aiExplanationRepository = aiExplanationRepository;
        this.aiPredictionRepository = aiPredictionRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * AI 예측 완료 후 비동기로 /explain 호출
     * - SAFE는 호출하지 않음
     * - 같은 trip에서 30초 이내 재호출 방지
     */
    public void explainAsync(String measurementId, Long tripId, String ruleRisk,
                             Map<String, Object> sensorContext) {
        if ("SAFE".equals(ruleRisk)) {
            return;
        }

        // 30초 쓰로틀링: 같은 trip에서 30초 이내면 건너뜀
        long now = System.currentTimeMillis();
        Long lastTime = lastExplainTime.get(tripId);
        if (lastTime != null && (now - lastTime) < EXPLAIN_INTERVAL_MS) {
            return;
        }
        lastExplainTime.put(tripId, now);

        explainExecutor.submit(() -> callExplain(measurementId, tripId, ruleRisk, sensorContext));
    }

    @SuppressWarnings("unchecked")
    private void callExplain(String measurementId, Long tripId, String ruleRisk,
                             Map<String, Object> sensorContext) {
        if (aiExplanationRepository.existsByMeasurementId(measurementId)) {
            return;
        }

        AiExplanation.AiExplanationBuilder builder = AiExplanation.builder()
                .measurementId(measurementId)
                .tripId(tripId)
                .ruleRisk(ruleRisk)
                .createdAt(LocalDateTime.now());

        try {
            List<AiPrediction> predictions = aiPredictionRepository.findByMeasurementId(measurementId);
            List<Map<String, Object>> aiPredictions = new ArrayList<>();
            for (AiPrediction p : predictions) {
                Map<String, Object> pred = new HashMap<>();
                pred.put("modelType", p.getModelType());
                pred.put("predictedRisk", p.getPredictedRisk() != null ? p.getPredictedRisk().name() : null);
                pred.put("predictedProbability", p.getPredictedProbability());
                pred.put("modelVersion", p.getModelVersion());
                aiPredictions.add(pred);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("measurementId", measurementId);
            requestBody.put("tripId", tripId);
            requestBody.put("ruleRisk", ruleRisk);
            requestBody.put("sensorContext", sensorContext);
            requestBody.put("aiPredictions", aiPredictions);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServerUrl + "/explain",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                builder.explanation((String) body.get("explanation"))
                        .source((String) body.get("source"))
                        .safetyControlAuthority((String) body.get("safetyControlAuthority"))
                        .status("EXPLAINED");
            } else {
                builder.status("ERROR")
                        .errorMessage("HTTP " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[AI Explain] 서버 연결 불가: {}", e.getMessage());
            builder.status("TIMEOUT")
                    .errorMessage(truncate(e.getMessage(), 500));

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 422) {
                builder.status("VALIDATION_ERROR")
                        .errorMessage(truncate("422: " + e.getResponseBodyAsString(), 500));
            } else {
                builder.status("ERROR")
                        .errorMessage(truncate("HTTP " + code + ": " + e.getMessage(), 500));
            }

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            builder.status("SERVER_ERROR")
                    .errorMessage(truncate(e.getStatusCode() + ": " + e.getResponseBodyAsString(), 500));

        } catch (Exception e) {
            log.error("[AI Explain] 에러: {}", e.getMessage());
            builder.status("ERROR")
                    .errorMessage(truncate(e.getMessage(), 500));
        }

        try {
            aiExplanationRepository.save(builder.build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("[AI Explain] 중복 저장 방지됨: {}", measurementId);
        } catch (Exception e) {
            log.error("[AI Explain] 저장 실패: {}", e.getMessage());
        }
    }

    public Optional<AiExplanation> findByMeasurementId(String measurementId) {
        return aiExplanationRepository.findByMeasurementId(measurementId);
    }

    public List<AiExplanation> findByTripId(Long tripId) {
        return aiExplanationRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "Unknown error";
        return s.length() <= max ? s : s.substring(0, max);
    }
}