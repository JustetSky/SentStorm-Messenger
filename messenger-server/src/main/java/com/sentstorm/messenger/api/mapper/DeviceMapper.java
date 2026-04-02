package com.sentstorm.messenger.api.mapper;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.api.dto.device.UserDevicePublicDto;
import com.sentstorm.messenger.core.entity.user.UserDevice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceDto toDto(UserDevice device);

    @Mapping(source = "deviceId", target = "deviceId")
    @Mapping(source = "publicKey", target = "publicKey")
    UserDevicePublicDto toPublicDto(UserDevice device);
}