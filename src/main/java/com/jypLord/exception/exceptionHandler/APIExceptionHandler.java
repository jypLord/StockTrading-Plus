package com.jypLord.exception.exceptionHandler;

import com.jypLord.exception.broker.KoreanMarketOverTimeException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class APIExceptionHandler {
    @ExceptionHandler(KoreanMarketOverTimeException.class)
    public ResponseEntity<Map<String, Object>> handleMarketTimeOver(KoreanMarketOverTimeException ex){

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", "한국 장 종료되어 실시간 주가 수신 불가능");
        errorBody.put("status", HttpStatus.BAD_REQUEST.value());
        errorBody.put("timestamp", LocalDateTime.now());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorBody);
    }
}
