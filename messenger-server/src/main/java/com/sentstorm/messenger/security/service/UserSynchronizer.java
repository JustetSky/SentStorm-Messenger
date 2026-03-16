package com.sentstorm.messenger.security.service;

import com.sentstorm.messenger.core.entity.User;
import com.sentstorm.messenger.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSynchronizer {

    private final UserRepository userRepository;

    public void synchronize(Jwt token) {

        UUID keycloakId = UUID.fromString(token.getSubject());

        var userOptional = userRepository.findByKeycloakId(keycloakId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLastSeen(Instant.now());
            userRepository.save(user);
            return;
        }

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setKeycloakId(keycloakId);

        user.setEmail(token.getClaim("email"));
        user.setFirstName(token.getClaim("given_name"));
        user.setLastName(token.getClaim("family_name"));

        user.setPublicId(token.getClaim("preferred_username"));

        user.setLastSeen(Instant.now());

        userRepository.save(user);
    }
}