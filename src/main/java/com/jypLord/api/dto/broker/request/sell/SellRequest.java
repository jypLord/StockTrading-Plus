package com.jypLord.api.dto.broker.request.sell;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SellRequest {
    BrokerageFirm firm;
    String stockCode;
    int price;
    int quantity;
    String oAuthToken;
}
