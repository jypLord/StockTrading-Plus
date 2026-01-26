package com.jypLord.domain.trade.dto.request.price;


import com.jypLord.api.BrokerageFirm;

public class LsReceivePriceRequest extends ReceivePriceRequest {
    public LsReceivePriceRequest(Long userId, BrokerageFirm firm) {
        super(userId, firm);
    }
}
