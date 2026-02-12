package com.jypLord.redis.streams.publisher;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.metrics.MarketDataMetrics;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Log4j2
@Component
@RequiredArgsConstructor
public class RedisStockEventPublisher {

    private final ReactiveRedisTemplate<String, String> redis;

    private static final String serverId = System.getenv().getOrDefault("HOSTNAME", "local").intern();

    private final MarketDataMetrics metrics;

    public Mono<Void> publishBrokerSessionTerminatedEvent(String stockCode) {
        String streamsKey = "ctrl:stock:" + stockCode;

        Map<String, String> body = Map.of(
            "stockCode", stockCode,
            "leaderServerId", serverId,
            "ts", String.valueOf(System.currentTimeMillis())
        );

        MapRecord<String, String, String> record =
            StreamRecords.mapBacked(body).withStreamKey(streamsKey);

        return redis.opsForStream().add(record)
            .doOnSuccess(a-> log.debug("주가 수신 종료로 재수신 요청 이벤트 발행"))
            .then();
    }

    public Mono<Void> publishLosscutEvent(Long userId, Long tradeId, String stockCode, BrokerageFirm firm, int losscutPrice, int quantity) {
        String streamsKey = "event:losscut:server:" + serverId;

        Map<String, String> body = Map.of(
            "idempotencyKey", tradeId.toString(),
            "userId",  userId.toString(),
            "stockCode", stockCode,
            "broker", firm.toString(),
            "losscutPrice", String.valueOf(losscutPrice),
            "quantity", String.valueOf(quantity)
        );

        MapRecord<String, String, String> record =
            StreamRecords.mapBacked(body).withStreamKey(streamsKey);

        return redis.opsForStream().add(record)
            .doOnSuccess(id -> metrics.rebuyEventEnqueued())
            .then();
    }
}
