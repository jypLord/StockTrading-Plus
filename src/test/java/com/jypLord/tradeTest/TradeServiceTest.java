package com.jypLord.tradeTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.buy.response.BuyResponse;
import com.jypLord.api.dto.request.sell.response.SellResponse;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.exception.trade.NoValidTradeException;
import com.jypLord.redis.pub.RedisAssetPricePublisher;
import com.jypLord.redis.streams.publisher.RedisStockEventPublisher;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import com.jypLord.redis.sub.StockPrice;
import java.rmi.AlreadyBoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private BrokerClient brokerClient;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisAssetPricePublisher redisPricePublisher;
    @Mock
    private RedisStockPriceSubscriber redisPriceSubscriber;
    @Mock
    private RedisStockEventPublisher redisStockEventPublisher;

    @Spy
    @InjectMocks
    private TradeService tradeService;

    @Test
    void registerTradeInfo_success() {
        RegisterTradeInfoRequest request = new RegisterTradeInfoRequest(1L, BrokerageFirm.LS, "005930", 90000, 1);

        given(tradeRepository.findByUserIdAndStockCodeAndStatus(1L, "005930", TradeStatus.ACTIVE)).willReturn(Mono.empty());
        given(tradeRepository.save(any(Trade.class))).willReturn(Mono.just(new Trade(1L, "005930", BrokerageFirm.LS, 90000, 1, TradeStatus.ACTIVE)));

        StepVerifier.create(tradeService.registerTradeInfo(request))
            .verifyComplete();
    }

    @Test
    void registerTradeInfo_duplicateTrade_throwsException() {
        RegisterTradeInfoRequest request = new RegisterTradeInfoRequest(1L, BrokerageFirm.LS, "005930", 90000, 1);
        Trade existing = new Trade(1L, "005930", BrokerageFirm.LS, 89000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findByUserIdAndStockCodeAndStatus(1L, "005930", TradeStatus.ACTIVE)).willReturn(Mono.just(existing));

        StepVerifier.create(tradeService.registerTradeInfo(request))
            .expectError(AlreadyBoundException.class)
            .verify();

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void manageAsset_noValidTrade_throwsException() {
        given(userRepository.findById(1L)).willReturn(Mono.just(new User(1L, "user@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, "token")));
        given(tradeRepository.findValidTradeByUserId(1L, BrokerageFirm.LS)).willReturn(Flux.empty());

        StepVerifier.create(tradeService.manageAsset(1L, BrokerageFirm.LS))
            .expectError(NoValidTradeException.class)
            .verify();
    }

    @Test
    void manageAsset_successFlow() {
        User user = new User(1L, "user@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, "market-token");
        Trade trade = new Trade(1L, "005930", BrokerageFirm.LS, 90000, 1, TradeStatus.ACTIVE);
        AssetPrice assetPrice = new AssetPrice("005930", 90500, 1L, BrokerageFirm.LS);

        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(tradeRepository.findValidTradeByUserId(1L, BrokerageFirm.LS)).willReturn(Flux.just(trade));
        given(redisPricePublisher.acquireLockIfAbsent("005930", 1L)).willReturn(Mono.just(true));
        given(brokerClient.receivePrice(any())).willReturn(Flux.just(assetPrice));
        given(redisPricePublisher.publishIfLockOwner(assetPrice)).willReturn(Mono.empty());
        given(tradeService.losscutMonitoring(eq(1L), anyLong(), eq("market-token"), eq(BrokerageFirm.LS), eq("005930"), eq(90000), eq(1)))
            .willReturn(Mono.empty());

        StepVerifier.create(tradeService.manageAsset(1L, BrokerageFirm.LS))
            .verifyComplete();
    }

    @Test
    void losscutMonitoring_priceDrops_executesSellAndEvent() {
        given(redisPriceSubscriber.subscribe("005930")).willReturn(Flux.just(new StockPrice("005930", 89000L)));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT)).willReturn(Mono.just(true));
        given(brokerClient.sell(any())).willReturn(Mono.just(org.mockito.Mockito.mock(SellResponse.class)));
        given(redisStockEventPublisher.publishLosscutEvent(1L, 10L, "005930", BrokerageFirm.LS, 90000, 1)).willReturn(Mono.empty());

        StepVerifier.create(tradeService.losscutMonitoring(1L, 10L, "token", BrokerageFirm.LS, "005930", 90000, 1))
            .verifyComplete();
    }

    @Test
    void updateTradeStatusForIdempotency_returnsRepositoryResult() {
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT)).willReturn(Mono.just(true));

        StepVerifier.create(tradeService.updateTradeStatusForIdempotency(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT))
            .assertNext(updated -> assertEquals(true, updated))
            .verifyComplete();
    }

    @Test
    void reBuyAfterLossCut_success() {
        User user = new User(1L, "user@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, "market-token");

        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.EXECUTED_LOSSCUT, TradeStatus.EXECUTED_BUY)).willReturn(Mono.just(true));
        given(brokerClient.buy(any())).willReturn(Mono.just(new BuyResponse("005930", 90000, 1)));

        Flux<StockPrice> prices = Flux.just(new StockPrice("005930", 91000L));

        StepVerifier.create(tradeService.reBuyAfterLossCut(10L, 1L, prices, BrokerageFirm.LS, 90000, 1))
            .verifyComplete();
    }

    @Test
    void reBuyAfterLossCut_idempotencyFalse_doesNotBuy() {
        User user = new User(1L, "user@test.com", "pw", "name", LocalDate.of(2000, 1, 1), null, "market-token");

        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.EXECUTED_LOSSCUT, TradeStatus.EXECUTED_BUY)).willReturn(Mono.just(false));

        StepVerifier.create(tradeService.reBuyAfterLossCut(10L, 1L, Flux.just(new StockPrice("005930", 91000L)), BrokerageFirm.LS, 90000, 1))
            .verifyComplete();

        verify(brokerClient, never()).buy(any());
    }
}
