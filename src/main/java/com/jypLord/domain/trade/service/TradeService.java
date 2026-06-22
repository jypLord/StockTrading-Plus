package com.jypLord.domain.trade.service;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.broker.request.buy.BuyRequest;
import com.jypLord.api.dto.broker.request.sell.SellRequest;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.dto.response.AssetPrice;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.exception.broker.BrokerException;
import com.jypLord.exception.trade.NoValidTradeException;
import com.jypLord.kafka.broker.SessionClosedEvent;
import com.jypLord.kafka.trade.TradeEventProducer;
import com.jypLord.kafka.trade.TradeType;
import com.jypLord.kafka.trade.event.EventType;
import com.jypLord.kafka.trade.event.TradeEvent;
import com.jypLord.redis.RedisWrapper;
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
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final BrokerClient brokerClient;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    private final RedisWrapper redisWrapper;
    private final TradeEventProducer tradeEventProducer;

    private final Map<Long, Set<String>> userSubscribeStockMap = new ConcurrentHashMap<>();

    public Mono<Void> registerTradeInfo(Long userId, RegisterTradeInfoRequest dto) {
        return tradeRepository.findMonitorableTradeByUserIdAndStockCode(userId, dto.stockCode())
            .flatMap(trade ->
                resolveTriggerPrice(userId, trade.getStockCode(), trade.getUserSetPrice())
                    .flatMap(currentTriggerPrice -> {
                        if (currentTriggerPrice == dto.price()) {
                            return Mono.error(new AlreadyBoundException("이미 있는 종목:" + dto.stockCode()));
                        }

                        return redisWrapper.saveTradeTriggerPrice(userId, dto.stockCode(), dto.price());
                    })
                    .thenReturn(Boolean.TRUE)
            )
            .switchIfEmpty(Mono.defer(() ->
                tradeRepository.save(
                        new Trade(userId, dto.stockCode(), dto.firm(), dto.price(), dto.quantity(), TradeStatus.ACTIVE))
                    .flatMap(saved -> redisWrapper.saveTradeTriggerPrice(userId, dto.stockCode(), dto.price()))
                    .thenReturn(Boolean.TRUE)
            ))
            .then();
    }

    public Flux<AssetPrice> receiveAssetInfo(Long userId, BrokerageFirm firm) {
        Mono<User> userCached = userRepository.findById(userId).cache();

        return tradeRepository.findValidTradeByUserId(userId, firm)
            .switchIfEmpty(Mono.error(new NoValidTradeException("유효한 데이터가 없음")))
            .take(10)
            .flatMap(trade ->
                redisWrapper.saveTradeTriggerPrice(userId, trade.getStockCode(), trade.getUserSetPrice())
                    .thenReturn(trade)
            )
            .filterWhen(trade -> redisWrapper.acquireStockLockIfAbsent(trade.getStockCode(), userId))
            .flatMap(trade ->
                userCached.flatMapMany(user ->
                    brokerClient.receivePrice(
                            DTOMapper.toPriceRequest(userId, firm, user.getMarketAccessToken(), trade.getStockCode())
                        )
                        .flatMap(asset ->
                            redisWrapper.publishPriceIfLockOwner(asset)
                                .thenReturn(asset)
                                .onErrorResume(
                                    error -> error instanceof BrokerException || error instanceof TimeoutException,
                                    error -> redisWrapper.removeStockLockIfOwner(asset.sourceUserId(), asset.stockCode())
                                        .then(redisWrapper.removeTradeTriggerPrice(userId, asset.stockCode()))
                                        .then(Mono.empty())
                                )
                        )
                )
            );
    }

    public Flux<Void> manageAsset(Long userId, BrokerageFirm firm) {
        return tradeRepository.findValidTradeByUserId(userId, firm)
            .switchIfEmpty(Mono.error(new NoValidTradeException("愿由ы븷 醫낅ぉ ?곗씠?곌? ?놁뒿?덈떎")))
            .take(10)
            .doOnNext(trade -> registerMonitoring(userId, trade.getStockCode()))
            .flatMap(trade ->
                losscutMonitoring(
                    userId,
                    trade.getId(),
                    firm,
                    trade.getStockCode(),
                    trade.getUserSetPrice(),
                    trade.getQuantity()
                ).flux()
            );
    }

    public Mono<Void> losscutMonitoring(
        Long userId,
        Long tradeId,
        BrokerageFirm firm,
        String stockCode,
        int userSetPrice,
        int quantity
    ) {
        return redisWrapper.subscribeStockPrice(stockCode)
            .flatMap(info ->
                resolveTriggerPrice(userId, stockCode, userSetPrice)
                    .filter(triggerPrice -> info.price() <= triggerPrice)
                    .map(triggerPrice -> Tuples.of(info, triggerPrice))
            )
            .take(1)
            .filterWhen(tuple -> startLosscutOrder(tradeId))
            .flatMap(tuple ->
                userRepository.findStockOAuthTokenById(userId)
                    .flatMap(token -> brokerClient.sell(new SellRequest(firm, stockCode, userSetPrice, quantity, token)))
                    .thenReturn(tuple)
            )
            .filterWhen(triggered -> updateTradeStatusForIdempotency(
                tradeId,
                TradeStatus.LOSSCUT_ORDER_SUBMITTED,
                TradeStatus.REBUY_WATCHING
            ))
            .flatMap(triggered -> tradeEventProducer.publishTradeEvent(
                    new TradeEvent(
                        EventType.TRADE_EVENT_OCCURRED,
                        "trade:%d:%s:%s".formatted(tradeId, TradeStatus.REBUY_WATCHING, TradeType.BUY),
                        tradeId,
                        userId,
                        stockCode,
                        firm,
                        userSetPrice,
                        quantity,
                        TradeType.BUY
                    )
                )
                .thenReturn(triggered))
            .doFinally(signalType -> unregisterMonitoring(userId, stockCode))
            .then();
    }

    public Mono<Void> rebuyMonitoring(
        Long userId,
        Long tradeId,
        BrokerageFirm firm,
        String stockCode,
        int userSetPrice,
        int quantity){
        return redisWrapper.subscribeStockPrice(stockCode)
            .flatMap(info ->
                resolveTriggerPrice(userId, stockCode, userSetPrice)
                    .filter(triggerPrice -> info.price() >= triggerPrice)
                    .map(triggerPrice -> Tuples.of(info, triggerPrice))
            )
            .take(1)
            .filterWhen(tuple -> updateTradeStatusForIdempotency(
                tradeId,
                TradeStatus.REBUY_WATCHING,
                TradeStatus.REBUY_ORDER_SUBMITTED
            ))
            .flatMap(tuple ->
                userRepository.findStockOAuthTokenById(userId)
                    .flatMap(token -> brokerClient.buy(new BuyRequest(firm, stockCode, userSetPrice, quantity, token)))
                    .thenReturn(tuple)
            )
            .filterWhen(triggered -> updateTradeStatusForIdempotency(
                tradeId,
                TradeStatus.REBUY_ORDER_SUBMITTED,
                TradeStatus.EXECUTED_BUY
            ))
            .flatMap(triggered -> tradeEventProducer.publishTradeEvent(
                    new TradeEvent(
                        EventType.TRADE_EVENT_OCCURRED,
                        "trade:%d:%s:%s".formatted(tradeId, TradeStatus.EXECUTED_BUY, TradeType.SELL),
                        tradeId,
                        userId,
                        stockCode,
                        firm,
                        userSetPrice,
                        quantity,
                        TradeType.SELL
                    )
                )
                .thenReturn(triggered))
            .doFinally(signalType -> unregisterMonitoring(userId, stockCode))
            .then();

    }

    public int currentMonitoringUserCount() {
        return userSubscribeStockMap.size();
    }

    public Mono<Boolean> updateTradeStatusForIdempotency(Long tradeId, TradeStatus expectedStatus, TradeStatus newStatus) {
        return tradeRepository.updateTradeStatus(tradeId, expectedStatus, newStatus);
    }

    public Flux<Trade> findByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status);
    }

    public Mono<Void> handleTradingSessionClosed(SessionClosedEvent event) {
        return redisWrapper.removeStockLockIfOwner(event.sourceUserId(), event.stockCode());
    }

    private void registerMonitoring(Long userId, String stockCode) {
        userSubscribeStockMap
            .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
            .add(stockCode);
    }

    private void unregisterMonitoring(Long userId, String stockCode) {
        userSubscribeStockMap.computeIfPresent(userId, (key, stockCodes) -> {
            stockCodes.remove(stockCode);
            return stockCodes.isEmpty() ? null : stockCodes;
        });
    }

    private Mono<Integer> resolveTriggerPrice(Long userId, String stockCode, int defaultPrice) {
        return redisWrapper.findTradeTriggerPrice(userId, stockCode)
            .defaultIfEmpty(defaultPrice);
    }

    private Mono<Boolean> startLosscutOrder(Long tradeId) {
        return updateTradeStatusForIdempotency(tradeId, TradeStatus.ACTIVE, TradeStatus.LOSSCUT_ORDER_SUBMITTED)
            .flatMap(updated -> updated
                ? Mono.just(true)
                : updateTradeStatusForIdempotency(tradeId, TradeStatus.EXECUTED_BUY, TradeStatus.LOSSCUT_ORDER_SUBMITTED)
            );
    }
}
