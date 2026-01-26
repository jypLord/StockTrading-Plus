package com.jypLord.api.dto.request.stockOAuth;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class StockOAuthRequest {
    final Long userId;
    final BrokerageFirm firm;
}
