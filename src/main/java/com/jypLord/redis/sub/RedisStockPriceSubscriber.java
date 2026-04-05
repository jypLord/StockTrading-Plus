package com.jypLord.redis.sub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Log4j2
@Component
@RequiredArgsConstructor
public class RedisStockPriceSubscriber {

    private final ReactiveRedisMessageListenerContainer container;

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    private static final class State {
        final Sinks.Many<StockPrice> sink =
            Sinks.many().multicast().directBestEffort();
        final AtomicInteger refCount = new AtomicInteger(0);
        volatile Disposable redisSub;
    }

    public Flux<StockPrice> subscribe(String stockCode) {
        return Flux.defer(() -> {

            State state = states.computeIfAbsent(stockCode, k -> new State());

            int count = state.refCount.incrementAndGet();
            if (count == 1) {
                startRedisSubscription(stockCode, state);
            }

            return state.sink.asFlux()
                .doFinally(sig -> release(stockCode));
        });
    }

    private void release(String stockCode) {

        State state = states.get(stockCode);
        if (state == null) {
            return;
        }

        int n = state.refCount.decrementAndGet();
        if (n > 0) {
            return;
        }

        if (n < 0) {
            state.refCount.compareAndSet(n, 0);
            return;
        }

        synchronized (state) {
            if (state.refCount.get() != 0) {
                return;
            }

            if (state.redisSub != null) {
                state.redisSub.dispose();
                state.redisSub = null;
            }

            state.sink.tryEmitComplete();
            states.remove(stockCode, state);
        }
    }

    private void startRedisSubscription(String stockCode, State state) {
        synchronized (state) {

            if (state.redisSub != null && !state.redisSub.isDisposed()) {
                return;
            }

            String channel = "stock:price:" + stockCode;

            state.redisSub = container.receive(ChannelTopic.of(channel))

                .map(msg -> toEvent(stockCode, msg))

                .doOnSubscribe(s -> log.info("주가 수신 시작={}", channel))
                .doOnError(e -> log.warn("주가 수신 중 에러 channel={}", channel, e))
                .doFinally(sig -> log.info("주가 수신 종료 channel={} signal={}", channel, sig))
                .subscribe(event -> {
                    Sinks.EmitResult r = state.sink.tryEmitNext(event);

                    if (r.isFailure()) {
                        log.debug("emit 실패 code={} result={}", stockCode, r);
                    }
                });
        }
    }

    public int activeBroadcastCount() {
        return states.size();
    }

    private StockPrice toEvent(String stockCode, Message<String, String> msg) {

        String body = msg.getMessage();

        long price = Long.parseLong(body.trim());

        return new StockPrice(stockCode, price);
    }
}
