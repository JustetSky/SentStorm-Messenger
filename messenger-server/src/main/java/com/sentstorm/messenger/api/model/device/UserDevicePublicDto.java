package com.sentstorm.messenger.api.model.device;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDevicePublicDto {

    private String deviceId;
    private String publicKey;
}