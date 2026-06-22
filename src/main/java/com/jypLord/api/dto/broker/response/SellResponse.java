package com.jypLord.api.dto.broker.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SellResponse {
    String stockCode;
    int price;
    int quantity;
}
