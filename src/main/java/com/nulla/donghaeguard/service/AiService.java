package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.AiResultRequest;
import com.nulla.donghaeguard.dto.response.AiDataResponse;
import com.nulla.donghaeguard.entity.RiskZone;
import com.nulla.donghaeguard.repository.RiskEventRepository;
import com.nulla.donghaeguard.repository.RiskZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RiskEventRepository riskEventRepository;
    private final RiskZoneRepository riskZoneRepository;

    @Transactional(readOnly = true)
    public List<AiDataResponse> getAiData() {
        return riskEventRepository.findAll().stream()
                .map(e -> new AiDataResponse(
                        e.getEventId(),
                        e.getSensorType().name(),
                        e.getRiskLevel().name(),
                        e.getDistanceMm(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getDetectedAt()
                ))
                .toList();
    }

    @Transactional
    public int saveAiResult(AiResultRequest request) {
        List<RiskZone> zones = request.getZones().stream()
                .map(z -> RiskZone.builder()
                        .latitude(z.getLatitude())
                        .longitude(z.getLongitude())
                        .riskScore(z.getRiskScore())
                        .riskGrade(RiskZone.RiskGrade.valueOf(z.getRiskGrade()))
                        .zoneEventCount(z.getZoneEventCount())
                        .reason(z.getReason())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .toList();

        riskZoneRepository.saveAll(zones);

        return zones.size();
    }
}