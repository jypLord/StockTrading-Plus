package com.jypLord.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jypLord.api.BrokerageFirm;
import lombok.Getter;

@Getter
public class LsStockOAuthSaveRequest extends StockOAuthSaveRequest {
    private final String appKey;
    private final String appSecretKey;

    @JsonCreator
    public LsStockOAuthSaveRequest(
        @JsonProperty("firm") BrokerageFirm firm,
        @JsonProperty("appKey") String appKey,
        @JsonProperty("appSecretKey") String appSecretKey
    ) {
        super(firm);
        this.appKey = appKey;
        this.appSecretKey = appSecretKey;
    }
}
