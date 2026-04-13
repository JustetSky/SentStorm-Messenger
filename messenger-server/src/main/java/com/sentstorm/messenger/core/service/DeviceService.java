package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.model.device.DeviceDto;
import com.sentstorm.messenger.api.model.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.model.device.UserDevicePublicDto;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    DeviceDto register(DeviceRegisterRequest request);

    void delete(UUID deviceId);

    List<UserDevicePublicDto> getUserDevices(String publicId);
}