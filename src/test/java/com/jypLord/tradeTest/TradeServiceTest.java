package com.jypLord.tradeTest;


import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.entity.Trade;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;


    @InjectMocks
    TradeService tradeService;

    private final RegisterTradeInfoRequest request =
        new RegisterTradeInfoRequest(1L, BrokerageFirm.LS, "005930", 99000, 1);
    @Test
    void registerTradeInfo_성공() {

        Trade saved = new Trade(
            1L, "005930", BrokerageFirm.LS,
            99000, 1, TradeStatus.ACTIVE
        );

        // 모든 매개변수 any() 로 통일
        given(tradeRepository.existsByUserIdAndStockCodeAndTradeStatus(
            any(), any(), any()
        )).willReturn(Mono.just(false));

        given(tradeRepository.save(any()))
            .willReturn(Mono.just(saved));

        Mono<Void> result = tradeService.registerTradeInfo(request);

        StepVerifier.create(result)
            .verifyComplete();

        then(tradeRepository).should(times(1))
            .existsByUserIdAndStockCodeAndTradeStatus(any(), any(), any());

        then(tradeRepository).should(times(1))
            .save(any());
    }


}
