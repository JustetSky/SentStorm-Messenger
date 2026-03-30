package com.sentstorm.messenger.api.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegisterRequest {

    @NotBlank
    private String deviceId;

    @NotBlank
    private String publicKey;
}
