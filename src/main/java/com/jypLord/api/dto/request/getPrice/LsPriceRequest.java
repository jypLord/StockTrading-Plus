package com.jypLord.api.dto.request.getPrice;


import com.jypLord.api.BrokerageFirm;
import java.util.List;

public class LsPriceRequest extends PriceRequest {

    public LsPriceRequest(Long userId, BrokerageFirm firm, String stockAccessToken, String stockCode) {
        super(userId, firm, stockAccessToken, stockCode);
    }
}
