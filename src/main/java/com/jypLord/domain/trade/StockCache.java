package com.jypLord.domain.trade;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockCache {
    final BrokerageFirm firm;
    final String stockCode;
    final int price;
    final int quantity;
    final String oAuthToken;
}
