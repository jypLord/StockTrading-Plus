package com.jypLord.domain.trade.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jypLord.api.dto.broker.response.BuyResponse;
import com.jypLord.api.dto.broker.response.SellResponse;
import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.kafka.trade.TradeEventProducer;
import com.jypLord.kafka.trade.TradeType;
import com.jypLord.kafka.trade.event.TradeEvent;
import com.jypLord.redis.RedisWrapper;
import com.jypLord.redis.sub.StockPrice;
import java.rmi.AlreadyBoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private RedisWrapper redisWrapper;
    @Mock
    private TradeEventProducer tradeEventProducer;

    @InjectMocks
    private TradeService tradeService;

    private final RegisterTradeInfoRequest request =
        new RegisterTradeInfoRequest(BrokerageFirm.LS, "005930", 99000, 1);

    @Test
    void registerTradeInfo_savesWhenNoActiveTradeExists() {
        Trade saved = new Trade(1L, "005930", BrokerageFirm.LS, 99000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findMonitorableTradeByUserIdAndStockCode(1L, "005930"))
            .willReturn(Mono.empty());
        given(tradeRepository.save(any(Trade.class))).willReturn(Mono.just(saved));
        given(redisWrapper.saveTradeTriggerPrice(1L, "005930", 99000)).willReturn(Mono.empty());

        StepVerifier.create(tradeService.registerTradeInfo(1L, request))
            .verifyComplete();

        verify(tradeRepository).save(any(Trade.class));
        verify(redisWrapper).saveTradeTriggerPrice(1L, "005930", 99000);
    }

    @Test
    void registerTradeInfo_throwsWhenSamePriceAlreadyExists() {
        Trade existing = new Trade(1L, "005930", BrokerageFirm.LS, 99000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findMonitorableTradeByUserIdAndStockCode(1L, "005930"))
            .willReturn(Mono.just(existing));
        given(redisWrapper.findTradeTriggerPrice(1L, "005930")).willReturn(Mono.just(99000));

        StepVerifier.create(tradeService.registerTradeInfo(1L, request))
            .expectError(AlreadyBoundException.class)
            .verify();

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void registerTradeInfo_updatesRegistryWhenActiveTradeExistsWithDifferentPrice() {
        Trade existing = new Trade(1L, "005930", BrokerageFirm.LS, 97000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findMonitorableTradeByUserIdAndStockCode(1L, "005930"))
            .willReturn(Mono.just(existing));
        given(redisWrapper.findTradeTriggerPrice(1L, "005930")).willReturn(Mono.just(97000));
        given(redisWrapper.saveTradeTriggerPrice(1L, "005930", 99000)).willReturn(Mono.empty());

        StepVerifier.create(tradeService.registerTradeInfo(1L, request))
            .verifyComplete();

        verify(tradeRepository, never()).save(any());
        verify(redisWrapper).saveTradeTriggerPrice(1L, "005930", 99000);
    }

    @Test
    void updateTradeStatusForIdempotency_delegatesToRepository() {
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT))
            .willReturn(Mono.just(true));

        StepVerifier.create(
                tradeService.updateTradeStatusForIdempotency(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT)
            )
            .expectNext(true)
            .verifyComplete();

        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.EXECUTED_LOSSCUT);
    }

    @Test
    void losscutMonitoring_sellsAndPublishesRebuyMonitoringEvent() {
        given(redisWrapper.subscribeStockPrice("005930"))
            .willReturn(Flux.just(new StockPrice("005930", 98000)));
        given(redisWrapper.findTradeTriggerPrice(1L, "005930")).willReturn(Mono.just(99000));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.LOSSCUT_ORDER_SUBMITTED))
            .willReturn(Mono.just(true));
        given(userRepository.findStockOAuthTokenById(1L)).willReturn(Mono.just("token"));
        given(brokerClient.sell(any())).willReturn(Mono.just(new SellResponse()));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.LOSSCUT_ORDER_SUBMITTED, TradeStatus.REBUY_WATCHING))
            .willReturn(Mono.just(true));
        given(tradeEventProducer.publishTradeEvent(any())).willReturn(Mono.empty());

        StepVerifier.create(tradeService.losscutMonitoring(1L, 10L, BrokerageFirm.LS, "005930", 99000, 1))
            .verifyComplete();

        ArgumentCaptor<TradeEvent> eventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(brokerClient).sell(any());
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.LOSSCUT_ORDER_SUBMITTED);
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.LOSSCUT_ORDER_SUBMITTED, TradeStatus.REBUY_WATCHING);
        verify(redisWrapper, never()).removeTradeTriggerPrice(1L, "005930");
        verify(tradeEventProducer).publishTradeEvent(eventCaptor.capture());
        assertEquals(TradeType.BUY, eventCaptor.getValue().tradeType());
    }

    @Test
    void rebuyMonitoring_buysAndPublishesLosscutMonitoringEvent() {
        given(redisWrapper.subscribeStockPrice("005930"))
            .willReturn(Flux.just(new StockPrice("005930", 100000)));
        given(redisWrapper.findTradeTriggerPrice(1L, "005930")).willReturn(Mono.just(99000));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.REBUY_WATCHING, TradeStatus.REBUY_ORDER_SUBMITTED))
            .willReturn(Mono.just(true));
        given(userRepository.findStockOAuthTokenById(1L)).willReturn(Mono.just("token"));
        given(brokerClient.buy(any())).willReturn(Mono.just(new BuyResponse("005930", 99000, 1)));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.REBUY_ORDER_SUBMITTED, TradeStatus.EXECUTED_BUY))
            .willReturn(Mono.just(true));
        given(tradeEventProducer.publishTradeEvent(any())).willReturn(Mono.empty());

        StepVerifier.create(tradeService.rebuyMonitoring(1L, 10L, BrokerageFirm.LS, "005930", 99000, 1))
            .verifyComplete();

        ArgumentCaptor<TradeEvent> eventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(brokerClient).buy(any());
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.REBUY_WATCHING, TradeStatus.REBUY_ORDER_SUBMITTED);
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.REBUY_ORDER_SUBMITTED, TradeStatus.EXECUTED_BUY);
        verify(redisWrapper, never()).removeTradeTriggerPrice(1L, "005930");
        verify(tradeEventProducer).publishTradeEvent(eventCaptor.capture());
        assertEquals(TradeType.SELL, eventCaptor.getValue().tradeType());
    }

    @Test
    void losscutMonitoring_startsFromExecutedBuyForNextCycle() {
        given(redisWrapper.subscribeStockPrice("005930"))
            .willReturn(Flux.just(new StockPrice("005930", 98000)));
        given(redisWrapper.findTradeTriggerPrice(1L, "005930")).willReturn(Mono.just(99000));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.LOSSCUT_ORDER_SUBMITTED))
            .willReturn(Mono.just(false));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.EXECUTED_BUY, TradeStatus.LOSSCUT_ORDER_SUBMITTED))
            .willReturn(Mono.just(true));
        given(userRepository.findStockOAuthTokenById(1L)).willReturn(Mono.just("token"));
        given(brokerClient.sell(any())).willReturn(Mono.just(new SellResponse()));
        given(tradeRepository.updateTradeStatus(10L, TradeStatus.LOSSCUT_ORDER_SUBMITTED, TradeStatus.REBUY_WATCHING))
            .willReturn(Mono.just(true));
        given(tradeEventProducer.publishTradeEvent(any())).willReturn(Mono.empty());

        StepVerifier.create(tradeService.losscutMonitoring(1L, 10L, BrokerageFirm.LS, "005930", 99000, 1))
            .verifyComplete();

        verify(brokerClient).sell(any());
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.ACTIVE, TradeStatus.LOSSCUT_ORDER_SUBMITTED);
        verify(tradeRepository).updateTradeStatus(10L, TradeStatus.EXECUTED_BUY, TradeStatus.LOSSCUT_ORDER_SUBMITTED);
    }

}
