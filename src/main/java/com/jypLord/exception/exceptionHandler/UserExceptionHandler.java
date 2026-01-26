package com.jypLord.exception.exceptionHandler;

import com.jypLord.exception.user.DuplicateSignUpException;
import com.jypLord.exception.user.FailedSaveRefreshTokenException;
import com.jypLord.exception.user.InvalidPasswordException;
import com.jypLord.exception.user.NoUserLoginException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(DuplicateSignUpException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatedSignUp(DuplicateSignUpException ex) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "DuplicateSignUp");
        errorBody.put("message", ex.getMessage());
        errorBody.put("status", HttpStatus.CONFLICT.value());
        errorBody.put("timestamp", LocalDateTime.now());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(errorBody);
    }

    @ExceptionHandler(NoUserLoginException.class)
    public ResponseEntity<String> handleNoUserLogin(NoUserLoginException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("없는 이메일: " + ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<String> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body("비밀번호 틀림: " + ex.getMessage());
    }

    @ExceptionHandler(FailedSaveRefreshTokenException.class)
    public ResponseEntity<String> handleFailedSaveRefreshToken(FailedSaveRefreshTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("<UNK> <UNK>: " + ex.getMessage());
    }
}