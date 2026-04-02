package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.api.dto.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.dto.device.UpdatePushTokenRequest;

import java.util.UUID;

public interface DeviceService {

    DeviceDto register(DeviceRegisterRequest request);
    
    void updatePushToken(UpdatePushTokenRequest request);
    
    void delete(UUID deviceId);
    
}
