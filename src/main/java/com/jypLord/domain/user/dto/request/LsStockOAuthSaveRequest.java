package com.jypLord.domain.user.dto.request;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter

public class LsStockOAuthSaveRequest extends StockOAuthSaveRequest{
    String appKey;
    String appSecretKey;

    public LsStockOAuthSaveRequest(BrokerageFirm firm, Long userId, String appKey,String appSecretKey){
        super(firm, userId);
        this.appKey=appKey;
        this.appSecretKey=appSecretKey;

    }
}
