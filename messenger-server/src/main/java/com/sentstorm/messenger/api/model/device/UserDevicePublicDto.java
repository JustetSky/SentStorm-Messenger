package com.sentstorm.messenger.api.model.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Public device information for E2E encryption")
public class UserDevicePublicDto {

    @Schema(description = "Unique device identifier", example = "f3c533c4-2df1-450a-a8dc-7d64abd9b0e4")
    private String deviceId;

    @Schema(description = "Public key for E2E encryption (Base64 encoded)", example = "M+O3GFx17cOcufauS7m4YAZdGWGkZL...")
    private String publicKey;
}