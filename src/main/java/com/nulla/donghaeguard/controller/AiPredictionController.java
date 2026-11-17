package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.entity.AiPrediction;
import com.nulla.donghaeguard.service.AiPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-predictions")
@RequiredArgsConstructor
public class AiPredictionController {

    private final AiPredictionService aiPredictionService;

    /** 특정 trip의 AI 예측 결과 전체 조회 */
    @GetMapping
    public List<AiPrediction> getByTrip(@RequestParam Long tripId) {
        return aiPredictionService.findByTripId(tripId);
    }

    /** 특정 measurement의 AI 예측 결과 조회 */
    @GetMapping("/{measurementId}")
    public List<AiPrediction> getByMeasurement(@PathVariable String measurementId) {
        return aiPredictionService.findByMeasurementId(measurementId);
    }
}
