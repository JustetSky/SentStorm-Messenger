package com.sentstorm.messenger.api.mapper;

import com.sentstorm.messenger.api.model.message.MessageDto;
import com.sentstorm.messenger.core.entity.message.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "state", expression = "java(message.getState().name())")
    MessageDto toDto(Message message);
}