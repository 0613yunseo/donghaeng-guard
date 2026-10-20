package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    List<AiPrediction> findByMeasurementId(String measurementId);
    List<AiPrediction> findByTripIdOrderByPredictedAtDesc(Long tripId);
}
