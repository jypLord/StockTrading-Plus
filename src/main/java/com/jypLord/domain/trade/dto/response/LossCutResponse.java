package com.jypLord.domain.trade.dto.response;

public record LossCutResponse(int stockCode, int price, int quantity, boolean cutBoolean,boolean reBuyOption) {
}
