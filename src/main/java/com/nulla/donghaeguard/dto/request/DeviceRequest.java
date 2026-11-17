package com.nulla.donghaeguard.dto.request;

import lombok.Getter;
import java.util.List;

@Getter
public class DeviceRequest {
    private Long userId;
    private String deviceId;
    private String deviceName;
    private List<String> sensorTypes;
}