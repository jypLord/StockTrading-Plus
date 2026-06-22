package com.jypLord.api.dto.broker.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuyResponse {
    String stockCode;
    int price;
    int quantity;
}
