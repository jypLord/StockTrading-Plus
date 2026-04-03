package com.jypLord.domain.trade.dto.request;

import com.jypLord.api.BrokerageFirm;
import jakarta.validation.constraints.NotNull;

public record RegisterTradeInfoRequest(@NotNull BrokerageFirm firm, @NotNull String stockCode, int price, int quantity) {
}
