package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.CameraEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CameraEventRepository extends JpaRepository<CameraEvent, Long> {
    Optional<CameraEvent> findBySensorEventEventId(Long eventId);
}