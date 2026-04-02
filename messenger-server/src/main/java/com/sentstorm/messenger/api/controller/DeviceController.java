package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.model.device.DeviceDto;
import com.sentstorm.messenger.api.model.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.model.device.UpdatePushTokenRequest;
import com.sentstorm.messenger.core.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping(ApiPath.DEVICES)
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Device endpoints")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @Operation(summary = "Register device")
    public ResponseEntity<DeviceDto> register(
            @RequestBody @Valid DeviceRegisterRequest request
    ) {
        return ResponseEntity.ok(deviceService.register(request));
    }

    @PutMapping(ApiPath.PUSH_TOKEN)
    @Operation(summary = "Update push token")
    public ResponseEntity<Void> updatePushToken(
            @RequestBody @Valid UpdatePushTokenRequest request
    ) {
        deviceService.updatePushToken(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(ApiPath.DEVICE_ID)
    @Operation(summary = "Delete device")
    public ResponseEntity<Void> delete(@PathVariable UUID deviceId) {
        deviceService.delete(deviceId);
        return ResponseEntity.noContent().build();
    }
}