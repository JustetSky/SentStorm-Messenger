package com.sentstorm.messenger.api.mapper;

import com.sentstorm.messenger.api.dto.device.DeviceDto;
import com.sentstorm.messenger.core.entity.user.UserDevice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "deviceId", target = "deviceId")
    @Mapping(source = "publicKey", target = "publicKey")
    DeviceDto toDto(UserDevice device);
}