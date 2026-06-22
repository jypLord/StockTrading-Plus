package com.jypLord.api.dto.broker.request.buy;

import com.jypLord.api.BrokerageFirm;

public class LsBuyRequest extends BuyRequest {

    public LsBuyRequest(BrokerageFirm firm, String stockCode, int price, int quantity, String oAuthToken) {
        super(firm, stockCode, price, quantity, oAuthToken);
    }
}
