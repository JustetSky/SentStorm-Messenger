package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.model.device.DeviceDto;
import com.sentstorm.messenger.api.model.device.DeviceRegisterRequest;
import com.sentstorm.messenger.api.model.device.UserDevicePublicDto;
import com.sentstorm.messenger.api.mapper.DeviceMapper;
import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.entity.device.UserDevice;
import com.sentstorm.messenger.core.exception.ErrorCode;
import com.sentstorm.messenger.core.exception.ServiceException;
import com.sentstorm.messenger.core.repository.device.UserDeviceRepository;
import com.sentstorm.messenger.core.repository.user.UserRepository;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final UserDeviceRepository deviceRepository;
    private final CurrentUserService currentUserService;
    private final DeviceMapper deviceMapper;
    private final UserRepository userRepository;

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

    @Override
    public void delete(UUID deviceId) {

        User currentUser = currentUserService.getCurrentUser();

        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.NOT_FOUND,
                        "Device not found"
                ));

        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new ServiceException(
                    ErrorCode.FORBIDDEN,
                    "Access denied"
            );
        }

        deviceRepository.delete(device);
    }

    @Override
    public List<UserDevicePublicDto> getUserDevices(String publicId) {

        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.NOT_FOUND,
                        "User not found"
                ));

        List<UserDevice> devices = deviceRepository
                .findByUserIdAndIsActiveTrue(user.getId());

        return devices.stream()
                .map(deviceMapper::toPublicDto)
                .toList();
    }
    
}