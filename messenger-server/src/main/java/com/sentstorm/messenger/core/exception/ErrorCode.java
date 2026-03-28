package com.sentstorm.messenger.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED),
    FORBIDDEN(HttpStatus.FORBIDDEN);

    private final HttpStatus httpStatus;
}