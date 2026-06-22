package com.jypLord.api.dto.broker.request.stockOAuth;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockOAuthRequest {
    final Long userId;
    final BrokerageFirm firm;
}
