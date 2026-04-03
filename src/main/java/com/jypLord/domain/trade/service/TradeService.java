package com.jypLord.domain.trade.service;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.api.dto.request.buy.request.BuyRequest;
import com.jypLord.api.dto.request.sell.SellRequest;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.exception.broker.BrokerException;
import com.jypLord.exception.trade.NoValidTradeException;
import com.jypLord.redis.pub.RedisAssetPricePublisher;
import com.jypLord.redis.streams.publisher.RedisStockEventPublisher;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import com.jypLord.redis.sub.StockPrice;
import com.jypLord.util.DTOMapper;
import java.rmi.AlreadyBoundException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final BrokerClient brokerClient;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final RedisAssetPricePublisher redisPricePublisher;
    private final RedisStockPriceSubscriber redisPriceSubscriber;
    private final RedisStockEventPublisher redisStockEventPublisher;

    private final Map<Long, Set<String>> userSubscribeStockMap = new ConcurrentHashMap<>();

    public Mono<Void> registerTradeInfo(Long userId, RegisterTradeInfoRequest dto) {
        return tradeRepository.findByUserIdAndStockCodeAndStatus(userId, dto.stockCode(), TradeStatus.ACTIVE)
            .flatMap(trade -> {
                if (trade.getUserSetPrice() == dto.price()) {
                    return Mono.error(new AlreadyBoundException("이미 추가한 종목:" + dto.stockCode()));
                }
                return Mono.error(new AlreadyBoundException());
            })
            .switchIfEmpty(
                tradeRepository.save(
                    new Trade(userId, dto.stockCode(), dto.firm(), dto.price(), dto.quantity(), TradeStatus.ACTIVE)
                )
            )
            .then();
    }

    public Flux<AssetPrice> receiveAssetInfo(Long userId, BrokerageFirm firm) {
        Mono<User> userCached = userRepository.findById(userId).cache();

        return tradeRepository.findValidTradeByUserId(userId, firm)
            .switchIfEmpty(Mono.error(new NoValidTradeException("저장된 종목이 없음")))
            .take(10)
            .doOnNext(trade ->
                userSubscribeStockMap
                    .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                    .add(trade.getStockCode())
            )
            .filterWhen(trade -> redisPricePublisher.acquireLockIfAbsent(trade.getStockCode(), userId))
            .flatMap(trade ->
                userCached.flatMapMany(user ->
                        brokerClient.receivePrice(
                            DTOMapper.toPriceRequest(userId, firm, user.getMarketAccessToken(), trade.getStockCode())
                        )
                        .flatMap(asset ->
                            redisPricePublisher.publishIfLockOwner(asset)
                                .thenReturn(asset)
                                .onErrorResume(
                                    error -> error instanceof BrokerException || error instanceof TimeoutException,
                                    error -> redisPricePublisher.removeLockIfOwner(asset.sourceUserId(), asset.stockCode())
                                        .then(redisStockEventPublisher.publishBrokerSessionTerminatedEvent(asset.stockCode()))
                                        .then(Mono.empty())
                                )
                        )
                )
            );
    }

    public Flux<Void> manageAsset(Long userId, BrokerageFirm firm) {
        Mono<User> userCached = userRepository.findById(userId).cache();

        return tradeRepository.findValidTradeByUserId(userId, firm)
            .switchIfEmpty(Mono.error(new NoValidTradeException("저장된 종목이 없음")))
            .take(10)
            .doOnNext(trade ->
                userSubscribeStockMap
                    .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                    .add(trade.getStockCode())
            )
            .flatMap(trade ->
                userCached.flatMapMany(user ->
                    losscutMonitoring(
                        userId,
                        trade.getId(),
                        user.getMarketAccessToken(),
                        firm,
                        trade.getStockCode(),
                        trade.getUserSetPrice(),
                        trade.getQuantity()
                    ).flux()
                )
            );
    }

    public Mono<Void> losscutMonitoring(
        Long userId,
        Long tradeId,
        String marketAccessToken,
        BrokerageFirm firm,
        String stockCode,
        int userSetPrice,
        int quantity
    ) {
        return redisPriceSubscriber.subscribe(stockCode)
            .filter(info -> info.price() <= userSetPrice)
            .take(1)
            .filterWhen(stock -> updateTradeStatusForIdempotency(tradeId, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT))
            .flatMap(info ->
                brokerClient.sell(new SellRequest(firm, stockCode, userSetPrice, quantity, marketAccessToken))
                    .and(redisStockEventPublisher.publishLosscutEvent(userId, tradeId, stockCode, firm, userSetPrice, quantity))
            )
            .then();
    }

    public Mono<Boolean> updateTradeStatusForIdempotency(Long tradeId, TradeStatus expectedStatus, TradeStatus newStatus) {
        return tradeRepository.updateTradeStatus(tradeId, expectedStatus, newStatus);
    }

    public Mono<Void> reBuyAfterLossCut(
        Long idempotencyKey,
        Long userId,
        Flux<StockPrice> currentPrice,
        BrokerageFirm firm,
        int losscutPrice,
        int quantity
    ) {
        return userRepository.findById(userId)
            .flatMapMany(user ->
                currentPrice
                    .filter(stock -> stock.price() > losscutPrice)
                    .next()
                    .filterWhen(event ->
                        updateTradeStatusForIdempotency(
                            idempotencyKey,
                            TradeStatus.EXECUTED_LOSSCUT,
                            TradeStatus.EXECUTED_BUY
                        )
                    )
                    .flatMap(risingStock ->
                        brokerClient.buy(
                            new BuyRequest(firm, risingStock.stockCode(), losscutPrice, quantity, user.getMarketAccessToken())
                        )
                    )
            )
            .then();
    }
}
