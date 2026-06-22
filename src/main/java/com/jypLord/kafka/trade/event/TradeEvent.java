package com.jypLord.kafka.trade.event;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.kafka.trade.TradeType;

public record TradeEvent(
    EventType eventType,
    String idempotencyKey,
    Long tradeId,
    Long userId,
    String stockCode,
    BrokerageFirm firm,
    int userSetPrice,
    int quantity,
    TradeType tradeType
) {
}
