package com.jypLord.kafka.trade;

import com.jypLord.domain.trade.service.TradeService;
import com.jypLord.kafka.broker.SessionClosedEvent;
import com.jypLord.kafka.trade.event.TradeEvent;
import com.jypLord.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final TradeService tradeService;

    @KafkaListener(
        topics = TradeEventProducer.TRADE_EVENTS_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTradeEvent(String message) {
        TradeEvent event = JsonUtil.fromJson(message, TradeEvent.class);

        handleTradeEvent(event)
            .doOnError(error -> log.error("Failed to handle trade event. idempotencyKey={}", event.idempotencyKey(), error))
            .subscribe();
    }

    @KafkaListener(topics = TradeEventProducer.SESSION_EVENTS_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSessionClosedEvent(String message) {
        SessionClosedEvent event =
            JsonUtil.fromJson(message, SessionClosedEvent.class);

        tradeService.handleTradingSessionClosed(event)
            .doOnError(error -> log.error("Failed to handle session closed event. idempotencyKey={}", event.idempotencyKey(), error))
            .subscribe();
    }

    private Mono<Void> handleTradeEvent(TradeEvent event) {
        if (event.tradeType().equals(TradeType.SELL)) {
            log.info("Sell trade event consumed. idempotencyKey={}, userId={}, stockCode={}",
                event.idempotencyKey(), event.userId(), event.stockCode());
            return tradeService.losscutMonitoring(
                event.userId(),
                event.tradeId(),
                event.firm(),
                event.stockCode(),
                event.userSetPrice(),
                event.quantity()
            );
        }

        log.info("Buy trade event consumed. idempotencyKey={}, userId={}, stockCode={}",
            event.idempotencyKey(), event.userId(), event.stockCode());
        return tradeService.rebuyMonitoring(
            event.userId(),
            event.tradeId(),
            event.firm(),
            event.stockCode(),
            event.userSetPrice(),
            event.quantity()
        );
    }
}
