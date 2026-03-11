package com.jypLord.exception.exceptionHandler;

import com.jypLord.exception.broker.BrokerException;
import com.jypLord.exception.broker.FailRetrievingStockInfoException;
import com.jypLord.exception.broker.KoreanMarketOverTimeException;
import com.jypLord.exception.broker.StockOAuthException;
import com.jypLord.exception.broker.WebSocketClosedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class APIExceptionHandler {

    @ExceptionHandler(KoreanMarketOverTimeException.class)
    public ResponseEntity<ErrorResponse> handleMarketTimeOver(KoreanMarketOverTimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "KOREAN_MARKET_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(FailRetrievingStockInfoException.class)
    public ResponseEntity<ErrorResponse> handleFailRetrievingStockInfo(FailRetrievingStockInfoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of(HttpStatus.BAD_GATEWAY, "FAIL_RETRIEVING_STOCK_INFO", ex.getMessage()));
    }

    @ExceptionHandler(StockOAuthException.class)
    public ResponseEntity<ErrorResponse> handleStockOAuthException(StockOAuthException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of(HttpStatus.BAD_GATEWAY, "STOCK_OAUTH_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(WebSocketClosedException.class)
    public ResponseEntity<ErrorResponse> handleWebSocketClosed(WebSocketClosedException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "WEBSOCKET_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(BrokerException.class)
    public ResponseEntity<ErrorResponse> handleBrokerException(BrokerException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of(HttpStatus.BAD_GATEWAY, "BROKER_ERROR", ex.getMessage()));
    }
}
