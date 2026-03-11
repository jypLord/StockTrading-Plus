package com.jypLord.exception.exceptionHandler;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
    String error,
    String message,
    int status,
    LocalDateTime timestamp
) {

    public static ErrorResponse of(HttpStatus status, String error, String message) {
        return new ErrorResponse(error, message, status.value(), LocalDateTime.now());
    }
}
