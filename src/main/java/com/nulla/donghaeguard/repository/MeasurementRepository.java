package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<Measurement, String> {
}
