package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.CameraEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraEventRepository extends JpaRepository<CameraEvent, Long> {
}