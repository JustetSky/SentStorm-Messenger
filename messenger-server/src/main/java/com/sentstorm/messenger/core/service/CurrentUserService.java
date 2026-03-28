package com.sentstorm.messenger.core.service;

import com.sentstorm.messenger.core.entity.user.User;
import com.sentstorm.messenger.core.repository.user.UserRepository;
import com.sentstorm.messenger.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {

        UUID keycloakId = SecurityUtils.getCurrentUserKeycloakId();

        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}