package com.jypLord.api.dto.broker.request.price;

import com.jypLord.api.BrokerageFirm;
import lombok.Getter;

@Getter
public class PriceRequest {
    private final Long userId;
    private final BrokerageFirm firm;
    private final String stockAccessToken;
    private final String stockCodes;

    public PriceRequest(Long userId, BrokerageFirm firm, String stockAccessToken, String stockCode) {
        this.userId = userId;
        this.firm = firm;
        this.stockAccessToken = stockAccessToken;
        this.stockCodes = stockCode;
    }

    public PriceRequest(Long userId, BrokerageFirm firm, String stockAccessToken) {
        this.userId = userId;
        this.firm = firm;
        this.stockAccessToken = stockAccessToken;
        this.stockCodes = null;
    }
}
