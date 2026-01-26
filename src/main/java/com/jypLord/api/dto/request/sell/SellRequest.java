package com.jypLord.api.dto.request.sell;


import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
public class SellRequest {
    BrokerageFirm firm;
    String stockCode;
    int price;
    int quantity;
    String oAuthToken;
}
