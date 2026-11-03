package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.AiExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiExplanationRepository extends JpaRepository<AiExplanation, Long> {
    Optional<AiExplanation> findByMeasurementId(String measurementId);
    List<AiExplanation> findByTripIdOrderByCreatedAtDesc(Long tripId);
    boolean existsByMeasurementId(String measurementId);
}
