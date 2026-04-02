package com.sentstorm.messenger.api.dto.device;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDevicePublicDto {

    private String deviceId;
    private String publicKey;
}