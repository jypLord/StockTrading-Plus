package com.jypLord.api.dto.broker.request.price;

import com.jypLord.api.BrokerageFirm;

public class LsPriceRequest extends PriceRequest {

    public LsPriceRequest(Long userId, BrokerageFirm firm, String stockAccessToken, String stockCode) {
        super(userId, firm, stockAccessToken, stockCode);
    }
}
