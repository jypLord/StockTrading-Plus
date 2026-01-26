package com.jypLord.redis.streams.handler;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.service.TradeService;
import com.jypLord.redis.streams.StreamKey;
import com.jypLord.redis.streams.StreamEnvelope;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import com.jypLord.redis.sub.RedisStockPriceSubscriber.StockPriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@Component
@RequiredArgsConstructor
public class LosscutEventHandler implements StreamEventHandler {

    private final TradeService tradeService;
    private final RedisStockPriceSubscriber priceSubscriber;

    @Override
    public StreamKey getKey() {
        return StreamKey.LOSSCUT_STREAM;
    }

    @Override
    public Mono<Void> handle(StreamEnvelope env) {

        Long userId = Long.parseLong(env.record().getValue().get("userId"));
        String stockCode = env.record().getValue().get("stockCode");

        BrokerageFirm firm = BrokerageFirm.valueOf(env.record().getValue().get("broker"));

        int losscutPrice = Integer.parseInt(env.record().getValue().get("losscutPrice"));
        int quantity = Integer.parseInt(env.record().getValue().get("quantity"));

        Flux<StockPriceEvent> curPrice = priceSubscriber.subscribe(stockCode);


        return tradeService.reBuyAfterLossCut(userId, curPrice, firm, losscutPrice, quantity);
    }
}
