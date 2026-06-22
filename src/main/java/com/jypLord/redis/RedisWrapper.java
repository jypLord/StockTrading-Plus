package com.jypLord.redis;

import com.jypLord.domain.trade.dto.response.AssetPrice;
import com.jypLord.redis.pub.RedisAssetPricePublisher;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import com.jypLord.redis.sub.StockPrice;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedisWrapper {
    private final RedisAssetPricePublisher publisher;
    private final RedisStockPriceSubscriber subscriber;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveRedisTemplate<String, Integer> integerRedisTemplate;


    public Mono<Boolean> acquireStockLockIfAbsent(String stockCode, Long userId) {
        return publisher.acquireLockIfAbsent(stockCode, userId);
    }

    public Mono<Void> publishPriceIfLockOwner(AssetPrice assetPrice) {
        return publisher.publishIfLockOwner(assetPrice);
    }

    public Mono<Void> removeStockLockIfOwner(Long userId, String stockCode) {
        return publisher.removeLockIfOwner(userId, stockCode);
    }

    // Redis Sub
    public Flux<StockPrice> subscribeStockPrice(String stockCode) {
        return subscriber.subscribe(stockCode);
    }


    // Redis Cache
    public Mono<String> getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(refreshTokenKey(email));
    }

    public Mono<Boolean> saveRefreshToken(String email, String refreshToken, Duration ttl) {
        return redisTemplate.opsForValue().set(refreshTokenKey(email), refreshToken, ttl);
    }

    public Mono<Integer> findTradeTriggerPrice(Long userId, String stockCode) {
        return integerRedisTemplate.opsForValue().get(tradeTriggerPriceKey(userId, stockCode));
    }

    public Mono<Void> saveTradeTriggerPrice(Long userId, String stockCode, int triggerPrice) {
        return integerRedisTemplate.opsForValue()
            .set(tradeTriggerPriceKey(userId, stockCode), triggerPrice)
            .then();
    }

    public Mono<Void> removeTradeTriggerPrice(Long userId, String stockCode) {
        return integerRedisTemplate.opsForValue()
            .delete(tradeTriggerPriceKey(userId, stockCode))
            .then();
    }

    

    public int activeStockPriceBroadcastCount() {
        return subscriber.activeBroadcastCount();
    }

    private String refreshTokenKey(String email) {
        return "refresh:" + email;
    }

    private String tradeTriggerPriceKey(Long userId, String stockCode) {
        return "trade:trigger:" + userId + ":" + stockCode;
    }
}
