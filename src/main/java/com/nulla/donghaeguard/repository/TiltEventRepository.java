package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.TiltEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiltEventRepository extends JpaRepository<TiltEvent, Long> {
}