package com.sentstorm.messenger.api.controller;

import com.sentstorm.messenger.api.constant.ApiPath;
import com.sentstorm.messenger.api.dto.PageResponse;
import com.sentstorm.messenger.api.dto.UserDto;
import com.sentstorm.messenger.api.dto.UserSearchDto;
import com.sentstorm.messenger.api.dto.UserSearchProjection;
import com.sentstorm.messenger.api.mapper.UserMapper;
import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.USERS)
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping(ApiPath.CURRENT_USER)
    @Operation(summary = "Get current authenticated user profile")
    public UserDto getCurrentUser() {

        User user = userService.getCurrentUser();

        return userMapper.toDto(user);
    }

    @GetMapping(ApiPath.PUBLIC_ID)
    @Operation(summary = "Get public information about a user")
    public UserDto getUserByPublicId(@PathVariable String publicId) {

        User user = userService.getUserByPublicId(publicId);

        return userMapper.toDto(user);
    }

    @GetMapping(ApiPath.SEARCH)
    @Operation(summary = "Search users by publicId")
    public PageResponse<UserSearchDto> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<UserSearchProjection> result =
                userService.searchUsers(query, PageRequest.of(page, size));

        return PageResponse.<UserSearchDto>builder()
                .items(result.map(userMapper::toSearchDto).getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .total(result.getTotalElements())
                .build();
    }

}