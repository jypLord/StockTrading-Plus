package com.jypLord.util;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.getPrice.LsPriceRequest;
import com.jypLord.api.dto.request.getPrice.PriceRequest;
import com.jypLord.api.dto.request.stockOAuth.LsStockOAuthRequest;
import com.jypLord.domain.user.dto.request.LsStockOAuthSaveRequest;
import com.jypLord.domain.user.dto.request.StockOAuthSaveRequest;

public class DTOMapper {

    public static LsStockOAuthRequest toStockOAuthRequest(Long userId, StockOAuthSaveRequest source) {
        switch (source.getFirm()) {
            case LS:
                LsStockOAuthSaveRequest dto = (LsStockOAuthSaveRequest) source;
                return new LsStockOAuthRequest(
                    userId,
                    dto.getFirm(),
                    dto.getAppKey(),
                    dto.getAppSecretKey()
                );
            case KIWOOM:
            default:
                throw new IllegalArgumentException("Unsupported brokerage firm: " + source.getFirm());
        }
    }

    public static PriceRequest toPriceRequest(Long userId, BrokerageFirm firm, String accessToken, String stockCode) {
        switch (firm) {
            case LS:
                return new LsPriceRequest(
                    userId,
                    firm,
                    accessToken,
                    stockCode
                );
            case KIWOOM:
            default:
                throw new IllegalArgumentException("Unsupported brokerage firm: " + firm);
        }
    }
}
