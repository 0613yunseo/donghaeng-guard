package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.response.RiskZoneResponse;
import com.nulla.donghaeguard.repository.RiskZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskZoneService {

    private final RiskZoneRepository riskZoneRepository;

    @Transactional(readOnly = true)
    public List<RiskZoneResponse> getRiskZones() {
        return riskZoneRepository.findAll().stream()
                .map(z -> new RiskZoneResponse(
                        z.getZoneId(),
                        z.getLatitude(),
                        z.getLongitude(),
                        z.getRiskScore(),
                        z.getRiskGrade().name(),
                        z.getZoneEventCount(),
                        z.getReason(),
                        z.getUpdatedAt()
                ))
                .toList();
    }
}