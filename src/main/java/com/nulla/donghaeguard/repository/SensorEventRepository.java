package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.SensorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensorEventRepository extends JpaRepository<SensorEvent, Long> {
    List<SensorEvent> findByTripTripId(Long tripId);
}