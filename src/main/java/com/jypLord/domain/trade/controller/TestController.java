package com.jypLord.domain.trade.controller;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.getPrice.LsPriceRequest;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.api.handler.LsBrokerClient;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Log4j2
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class TestController {

    private final LsBrokerClient lsBrokerClient;
    private final TradeService tradeService;

    @PostMapping("/settingTest")
    public Mono<ResponseEntity<Void>> test(BrokerageFirm firm, String code, int price, int quantity) {

        return tradeService.registerTradeInfo(new RegisterTradeInfoRequest(1L, firm, code, price, quantity))
            .thenReturn(ResponseEntity.ok().build());
    }
}
