package com.sentstorm.messenger.api.mapper;

import com.sentstorm.messenger.api.model.user.UserDto;
import com.sentstorm.messenger.api.model.user.UserSearchDto;
import com.sentstorm.messenger.api.model.user.UserSearchProjection;
import com.sentstorm.messenger.core.entity.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    UserSearchDto toSearchDto(UserSearchProjection projection);
}