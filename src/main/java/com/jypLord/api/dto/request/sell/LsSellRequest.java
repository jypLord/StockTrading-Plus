package com.jypLord.api.dto.request.sell;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class LsSellRequest extends SellRequest {

    public LsSellRequest(BrokerageFirm firm, String stockCode, int price, int quantity, String oAuthToken) {
        super(firm, stockCode, price, quantity, oAuthToken);
    }
}
