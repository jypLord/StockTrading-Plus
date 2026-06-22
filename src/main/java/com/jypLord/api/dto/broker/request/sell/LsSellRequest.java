package com.jypLord.api.dto.broker.request.sell;

import com.jypLord.api.BrokerageFirm;

public class LsSellRequest extends SellRequest {

    public LsSellRequest(BrokerageFirm firm, String stockCode, int price, int quantity, String oAuthToken) {
        super(firm, stockCode, price, quantity, oAuthToken);
    }
}
