package com.jypLord.api.dto.request;

import com.jypLord.api.BrokerageFirm;

public interface StockMarketRequest {
    Long getUserId();
    BrokerageFirm getFirm();
}
