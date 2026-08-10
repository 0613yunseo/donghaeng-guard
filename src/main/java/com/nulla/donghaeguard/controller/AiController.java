package com.nulla.donghaeguard.controller;

import com.nulla.donghaeguard.common.ApiResponse;
import com.nulla.donghaeguard.dto.request.AiResultRequest;
import com.nulla.donghaeguard.dto.response.AiDataResponse;
import com.nulla.donghaeguard.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/data")
    public ApiResponse<List<AiDataResponse>> getAiData() {
        List<AiDataResponse> response = aiService.getAiData();
        return ApiResponse.ok(response);
    }

    @PostMapping("/result")
    public ApiResponse<Map<String, Integer>> saveAiResult(@RequestBody AiResultRequest request) {
        int savedCount = aiService.saveAiResult(request);
        return ApiResponse.ok(Map.of("savedCount", savedCount));
    }
}