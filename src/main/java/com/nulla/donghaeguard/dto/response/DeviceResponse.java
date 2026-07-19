package com.nulla.donghaeguard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceResponse {
    private String deviceId;
    private String deviceStatus;
}