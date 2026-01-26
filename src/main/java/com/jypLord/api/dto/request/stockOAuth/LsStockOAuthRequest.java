package com.jypLord.api.dto.request.stockOAuth;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.user.dto.request.LsStockOAuthSaveRequest;
import lombok.Getter;

@Getter
public class LsStockOAuthRequest extends StockOAuthRequest {
    private final String appKey;
    private final String appSecretKey;

    public LsStockOAuthRequest(Long userId,BrokerageFirm firm ,String appKey, String appSecretKey) {
        super(userId,firm);
        this.appKey = appKey;
        this.appSecretKey = appSecretKey;
    }

}
