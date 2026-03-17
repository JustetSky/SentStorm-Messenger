package com.sentstorm.messenger.api.mapper;

import com.sentstorm.messenger.api.dto.UserDto;
import com.sentstorm.messenger.api.dto.UserSearchDto;
import com.sentstorm.messenger.api.dto.UserSearchProjection;
import com.sentstorm.messenger.core.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {

        return UserDto.builder()
                .publicId(user.getPublicId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .lastSeen(user.getLastSeen())
                .build();
    }

    public UserSearchDto toSearchDto(UserSearchProjection p) {
        return UserSearchDto.builder()
                .publicId(p.getPublicId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .lastSeen(p.getLastSeen())
                .build();
    }

}