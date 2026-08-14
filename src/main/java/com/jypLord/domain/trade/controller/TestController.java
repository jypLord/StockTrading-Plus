package com.jypLord.domain.trade.controller;

import com.jypLord.api.handler.impl.LsBrokerClient;
import com.jypLord.domain.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@Log4j2
@RestController
@RequiredArgsConstructor
public class TestController {

    private final LsBrokerClient lsBrokerClient;
    private final TradeService tradeService;

    @PostMapping("/test")
    public Mono<ResponseEntity<Void>> test(String json) {

        String a = new String(json);

        return Mono.just(ResponseEntity.ok().build());
    }
}
