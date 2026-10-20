package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.dto.request.MeasurementRequest;
import com.nulla.donghaeguard.dto.response.AiMeasurementResponse;
import com.nulla.donghaeguard.service.MeasurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    /** 센서 PC → 0.2초 판정 주기 1회분 통합 저장 */
    @PostMapping("/api/measurements")
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody MeasurementRequest request) {
        String id = measurementService.saveMeasurement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("measurementId", id));
    }

    /** AI 조회 (JSON): 측정 주기 1회 = 1행 */
    @GetMapping("/internal/ai/measurements")
    public List<AiMeasurementResponse> findForAi(@RequestParam(required = false) Long tripId,
                                                 @RequestParam(required = false) String deviceId) {
        return measurementService.findForAi(tripId, deviceId);
    }

    /** AI 조회 (CSV 다운로드) */
    @GetMapping("/internal/ai/measurements/csv")
    public ResponseEntity<String> downloadCsv(@RequestParam(required = false) Long tripId,
                                              @RequestParam(required = false) String deviceId) {
        String csv = measurementService.toCsv(measurementService.findForAi(tripId, deviceId));
        String filename = "measurements" + (tripId != null ? "_trip" + tripId : "") + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(csv);
    }
}
