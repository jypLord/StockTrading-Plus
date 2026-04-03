package com.jypLord.domain.trade.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.redis.pub.RedisAssetPricePublisher;
import com.jypLord.redis.streams.publisher.RedisStockEventPublisher;
import com.jypLord.redis.sub.RedisStockPriceSubscriber;
import java.rmi.AlreadyBoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private com.jypLord.api.handler.BrokerClient brokerClient;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisAssetPricePublisher redisAssetPricePublisher;
    @Mock
    private RedisStockPriceSubscriber redisStockPriceSubscriber;
    @Mock
    private RedisStockEventPublisher redisStockEventPublisher;

    @InjectMocks
    private TradeService tradeService;

    private final RegisterTradeInfoRequest request =
        new RegisterTradeInfoRequest(BrokerageFirm.LS, "005930", 99000, 1);

    @Test
    void registerTradeInfo_savesWhenNoActiveTradeExists() {
        Trade saved = new Trade(1L, "005930", BrokerageFirm.LS, 99000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findByUserIdAndStockCodeAndStatus(1L, "005930", TradeStatus.ACTIVE))
            .willReturn(Mono.empty());
        given(tradeRepository.save(any(Trade.class))).willReturn(Mono.just(saved));

        StepVerifier.create(tradeService.registerTradeInfo(1L, request))
            .verifyComplete();

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void registerTradeInfo_throwsWhenSamePriceAlreadyExists() {
        Trade existing = new Trade(1L, "005930", BrokerageFirm.LS, 99000, 1, TradeStatus.ACTIVE);

        given(tradeRepository.findByUserIdAndStockCodeAndStatus(1L, "005930", TradeStatus.ACTIVE))
            .willReturn(Mono.just(existing));

        StepVerifier.create(tradeService.registerTradeInfo(1L, request))
            .expectError(AlreadyBoundException.class)
            .verify();

        verify(tradeRepository, never()).save(any());
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
}
