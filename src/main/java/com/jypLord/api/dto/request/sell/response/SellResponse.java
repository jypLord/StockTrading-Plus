package com.jypLord.api.dto.request.sell.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SellResponse {
    String stockCode;

    int price;
    int quantity;
}
