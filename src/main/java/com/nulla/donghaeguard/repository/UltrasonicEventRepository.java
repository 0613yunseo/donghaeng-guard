package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.UltrasonicEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UltrasonicEventRepository extends JpaRepository<UltrasonicEvent, Long> {
    Optional<UltrasonicEvent> findBySensorEventEventId(Long eventId);
}