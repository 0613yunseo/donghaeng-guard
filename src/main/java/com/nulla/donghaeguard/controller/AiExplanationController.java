package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.entity.AiExplanation;
import com.nulla.donghaeguard.service.AiExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-explanations")
@RequiredArgsConstructor
public class AiExplanationController {

    private final AiExplanationService aiExplanationService;

    /** trip별 LLM 설명 조회 */
    @GetMapping
    public List<AiExplanation> getByTrip(@RequestParam Long tripId) {
        return aiExplanationService.findByTripId(tripId);
    }

    /** measurement별 LLM 설명 조회 */
    @GetMapping("/{measurementId}")
    public ResponseEntity<AiExplanation> getByMeasurement(@PathVariable String measurementId) {
        return aiExplanationService.findByMeasurementId(measurementId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
