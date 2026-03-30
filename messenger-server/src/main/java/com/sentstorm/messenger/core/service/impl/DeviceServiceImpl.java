package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.api.dto.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.mapper.DeviceMapper;
import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.entity.user.UserDevice;
import com.sentstorm.messenger.core.repository.device.UserDeviceRepository;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final UserDeviceRepository deviceRepository;
    private final CurrentUserService currentUserService;
    private final DeviceMapper deviceMapper;

    @Override
    public DeviceDto register(DeviceRegisterRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        UserDevice device = deviceRepository
                .findByUserIdAndDeviceId(currentUser.getId(), request.getDeviceId())
                .orElse(null);

        if (device == null) {
            device = UserDevice.builder()
                    .id(UUID.randomUUID())
                    .user(currentUser)
                    .deviceId(request.getDeviceId())
                    .createdDate(Instant.now())
                    .isActive(true)
                    .build();
        }

        device.setPublicKey(request.getPublicKey());
        device.setLastActive(Instant.now());
        device.setIsActive(true);

        device = deviceRepository.save(device);

        return deviceMapper.toDto(device);
    }
}