package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.api.dto.device.DeviceRegisterRequest;

public interface DeviceService {

    DeviceDto register(DeviceRegisterRequest request);
}
