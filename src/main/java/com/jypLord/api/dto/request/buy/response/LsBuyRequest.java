package com.jypLord.api.dto.request.buy.response;


import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.buy.request.BuyRequest;

public class LsBuyRequest extends BuyRequest {

    public LsBuyRequest(BrokerageFirm firm, String stockCode, int price, int quantity, String oAuthToken) {
        super(firm, stockCode, price, quantity, oAuthToken);
    }

}
