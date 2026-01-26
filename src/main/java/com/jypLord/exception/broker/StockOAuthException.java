package com.jypLord.exception.broker;

import lombok.Getter;


@Getter
public class StockOAuthException extends BrokerException {


    public StockOAuthException(String message) {
        super(message);
    }
}
