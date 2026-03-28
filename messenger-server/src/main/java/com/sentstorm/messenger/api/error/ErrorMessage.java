package com.sentstorm.messenger.api.error;

import com.sentstorm.messenger.core.exception.ErrorCode;

public record ErrorMessage(
        ErrorCode code,
        String message
) {}