package com.jypLord.metrics;


import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


@Component
public class MarketDataMetrics {

    // 현재 브로드캐스팅 중인 종목 수
    private final AtomicInteger activePriceBroadcasts = new AtomicInteger(0);

    // 손절 후 재매수 대기 이벤트 수
    private final AtomicInteger pendingRebuyEvents = new AtomicInteger(0);

    public MarketDataMetrics(MeterRegistry registry,   RedisStockPriceSubscriber subscriber) {
        registry.gauge("active_price_broadcasts", subscriber.activeBroadcastCount());

        registry.gauge("pending_rebuy_events", pendingRebuyEvents);
    }


    // ---- Streams 재매수 이벤트 ----
    public void rebuyEventEnqueued() {
        pendingRebuyEvents.incrementAndGet();
    }

    public void rebuyEventDone() {
        pendingRebuyEvents.decrementAndGet();
    }

}
