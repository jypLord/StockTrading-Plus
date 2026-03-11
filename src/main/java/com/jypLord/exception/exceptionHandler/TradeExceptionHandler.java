package com.jypLord.exception.exceptionHandler;

import com.jypLord.exception.trade.AlreadyExistTradeException;
import com.jypLord.exception.trade.NoValidTradeException;
import com.jypLord.exception.trade.TradeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TradeExceptionHandler {

    @ExceptionHandler(AlreadyExistTradeException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExistTrade(AlreadyExistTradeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(HttpStatus.CONFLICT, "TRADE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(NoValidTradeException.class)
    public ResponseEntity<ErrorResponse> handleNoValidTrade(NoValidTradeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "NO_VALID_TRADE", ex.getMessage()));
    }

    @ExceptionHandler(TradeException.class)
    public ResponseEntity<ErrorResponse> handleTradeException(TradeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "TRADE_ERROR", ex.getMessage()));
    }
}
