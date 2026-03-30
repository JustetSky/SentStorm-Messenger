package com.sentstorm.messenger.core.exception;

import com.sentstorm.messenger.api.error.ErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorMessage> handleServiceException(ServiceException ex) {

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(new ErrorMessage(
                        ex.getErrorCode(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(
            Exception ex,
            HttpServletRequest request
    ) throws Exception {

        String uri = request.getRequestURI();

        if (uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-ui")) {

            throw ex;
        }

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(new ErrorMessage(
                        ErrorCode.INTERNAL_ERROR,
                        "Something went wrong"
                ));
    }
}