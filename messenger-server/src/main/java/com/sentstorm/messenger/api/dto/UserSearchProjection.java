package com.sentstorm.messenger.api.dto;

import java.time.Instant;

public interface UserSearchProjection {

    String getPublicId();

    String getFirstName();

    String getLastName();

    Instant getLastSeen();

}