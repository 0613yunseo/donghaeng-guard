package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.UltrasonicEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UltrasonicEventRepository extends JpaRepository<UltrasonicEvent, Long> {
}