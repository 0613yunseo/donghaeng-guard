package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, String> {
}