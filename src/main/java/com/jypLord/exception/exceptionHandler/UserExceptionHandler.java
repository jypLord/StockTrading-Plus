package com.jypLord.exception.exceptionHandler;

import com.jypLord.exception.user.DuplicateSignUpException;
import com.jypLord.exception.user.FailedSaveRefreshTokenException;
import com.jypLord.exception.user.InvalidPasswordException;
import com.jypLord.exception.user.NoSuchUserException;
import com.jypLord.exception.user.NoUserLoginException;
import com.jypLord.exception.user.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(DuplicateSignUpException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatedSignUp(DuplicateSignUpException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(HttpStatus.CONFLICT, "DUPLICATE_SIGN_UP", ex.getMessage()));
    }

    @ExceptionHandler({NoUserLoginException.class, NoSuchUserException.class})
    public ResponseEntity<ErrorResponse> handleNoUserLogin(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", ex.getMessage()));
    }

    @ExceptionHandler(FailedSaveRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleFailedSaveRefreshToken(FailedSaveRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "FAILED_SAVE_REFRESH_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "USER_ERROR", ex.getMessage()));
    }
}
