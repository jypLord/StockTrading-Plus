package com.jypLord.api.dto.broker.request.buy;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BuyRequest {
    BrokerageFirm firm;
    String stockCode;
    int price;
    int quantity;
    String oAuthToken;
}
