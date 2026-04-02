package com.sentstorm.messenger.api.model.device;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeviceDto {

    private UUID id;
    private String deviceId;
    private String publicKey;
}