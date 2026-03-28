package com.sentstorm.messenger.core.service.impl;

import com.sentstorm.messenger.api.dto.user.UserSearchProjection;
import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.repository.user.UserRepository;
import com.sentstorm.messenger.core.service.CurrentUserService;
import com.sentstorm.messenger.core.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    @Override
    public User getUserByPublicId(String publicId) {

        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("User not found"));

    }

    @Override
    public Page<UserSearchProjection> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }

}