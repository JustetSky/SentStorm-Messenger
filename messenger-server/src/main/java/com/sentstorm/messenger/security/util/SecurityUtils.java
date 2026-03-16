package com.sentstorm.messenger.security.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserKeycloakId() {

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String sub = authentication.getToken().getSubject();

        return UUID.fromString(sub);
    }
}