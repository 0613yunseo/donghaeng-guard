package com.nulla.donghaeguard.service;

import com.nulla.donghaeguard.dto.request.DeviceRequest;
import com.nulla.donghaeguard.dto.response.DeviceResponse;
import com.nulla.donghaeguard.entity.Device;
import com.nulla.donghaeguard.entity.User;
import com.nulla.donghaeguard.repository.DeviceRepository;
import com.nulla.donghaeguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceResponse registerDevice(DeviceRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .user(user)
                .deviceName(request.getDeviceName())
                .sensorTypes(String.join(",", request.getSensorTypes()))
                .deviceStatus(Device.DeviceStatus.CONNECTED)
                .createdAt(LocalDateTime.now())
                .build();

        deviceRepository.save(device);

        return new DeviceResponse(device.getDeviceId(), device.getDeviceStatus().name());
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevices(Long userId) {
        return deviceRepository.findAll().stream()
                .filter(d -> d.getUser().getUserId().equals(userId))
                .map(d -> new DeviceResponse(d.getDeviceId(), d.getDeviceStatus().name()))
                .toList();
    }
}