package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.api.dto.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.dto.device.UpdatePushTokenRequest;
import com.sentstorm.messenger.api.dto.device.UserDevicePublicDto;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    DeviceDto register(DeviceRegisterRequest request);

    void updatePushToken(UpdatePushTokenRequest request);

    void delete(UUID deviceId);

    List<UserDevicePublicDto> getUserDevices(String publicId);
}