package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
    List<RiskEvent> findByTripTripId(Long tripId);
}