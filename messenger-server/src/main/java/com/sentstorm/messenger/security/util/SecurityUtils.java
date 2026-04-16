package com.sentstorm.messenger.security.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserKeycloakId() {
        var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            if (jwt != null && jwt.getSubject() != null) {
                return UUID.fromString(jwt.getSubject());
            }
        }

        throw new IllegalStateException("Unable to extract Keycloak ID from security context");
    }
}