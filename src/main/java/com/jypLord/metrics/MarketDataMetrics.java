package com.jypLord.metrics;

import com.jypLord.domain.trade.service.TradeService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MarketDataMetrics {

    private final AtomicInteger pendingRebuyEvents = new AtomicInteger(0);

    public MarketDataMetrics(MeterRegistry registry, TradeService tradeService) {
        registry.gauge("active_monitoring_users", tradeService, TradeService::currentMonitoringUserCount);
        registry.gauge("pending_rebuy_events", pendingRebuyEvents);
    }

    public void rebuyEventEnqueued() {
        pendingRebuyEvents.incrementAndGet();
    }

    public void rebuyEventDone() {
        pendingRebuyEvents.decrementAndGet();
    }
}
