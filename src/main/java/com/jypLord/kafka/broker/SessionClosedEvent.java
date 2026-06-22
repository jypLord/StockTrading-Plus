package com.jypLord.kafka.broker;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.kafka.trade.event.EventType;

public record SessionClosedEvent(
    EventType eventType,
    String idempotencyKey,
    Long sourceUserId,
    BrokerageFirm broker,
    String stockCode
) {
}