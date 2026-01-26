package com.jypLord.api.dto.request.buy.request;

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
