package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.TiltEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TiltEventRepository extends JpaRepository<TiltEvent, Long> {
    Optional<TiltEvent> findBySensorEventEventId(Long eventId);
}