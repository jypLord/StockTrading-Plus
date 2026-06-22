package com.jypLord.kafka.trade;


import com.jypLord.kafka.broker.SessionClosedEvent;
import com.jypLord.kafka.trade.event.TradeEvent;
import com.jypLord.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TradeEventProducer {

    public static final String TRADE_EVENTS_TOPIC = "trade.events";
    public static final String SESSION_EVENTS_TOPIC = "session.events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public Mono<Void> publishTradeEvent(TradeEvent event) {
        return Mono.fromFuture(kafkaTemplate.send(TRADE_EVENTS_TOPIC, "trade:" + event.userId(), JsonUtil.toJson(event)))
            .then();
    }

    public Mono<Void> publishSessionClosedEvent(SessionClosedEvent event) {
        return Mono.fromFuture(
                kafkaTemplate.send(SESSION_EVENTS_TOPIC, event.broker() + ":" + event.stockCode(), JsonUtil.toJson(event))
            )
            .then();
    }
}
