package com.jypLord.apiTest;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.getPrice.LsPriceRequest;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.api.handler.LsBrokerClient;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.time.Duration;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest(properties = {
    "jwt.secret.key=test-secret-test-secret-test-secret-test-secret"
})
class LsClientTest {

    @Autowired
    LsBrokerClient client;


    String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6IjZmNjAzZjk5LTA5MjYtNDI1MS1hOTMwLTQ0MmZlNDZmMzc3MSIsIm5iZiI6MTc2ODk1NTQyNywiZ3JhbnRfdHlwZSI6IkNsaWVudCIsImlzcyI6InVub2d3IiwiZXhwIjoxNzY5MDMyNzk5LCJpYXQiOjE3Njg5NTU0MjcsImp0aSI6IlBTcnc5QlpxaDFjNnZ4QWtzTm8xUzdzMlVLN21IYkJ0T0Z3TiJ9.NtpaPDsCswY6TwzAqrM8ETKlcECT_1u8vEn2TVlw9fqfjCptta5fyo9EnpGj_nsMg3r1UTvW8gV_LrIpo1K8Ew";


    @Test
    void websocket_price_stream_test() {

        String stockCode = "005930"; // 삼성전자

        Flux<AssetPrice> flux =
            client.receivePrice(new LsPriceRequest(1L, BrokerageFirm.LS, accessToken, stockCode))
                .doOnSubscribe(s -> log.info("SUBSCRIBE"))
                .doOnNext(p -> log.info("price={}", p.price()))
                .doOnError(e -> log.error("WS ERROR", e))
                .doFinally(sig -> log.info("FINALLY={}", sig));


        StepVerifier.create(flux.take(10))
            .expectNextCount(10)
            .verifyComplete();
    }
    @Test
    void polling_price_stream_test() {

        String stockCode = "005930";

        Flux<AssetPrice> flux =
            client.receivePriceViaPolling(1L, accessToken, stockCode)
                .doOnSubscribe(s -> log.info("SUBSCRIBE"))
                .doOnNext(p -> log.info("price={}", p.price()))
                .doOnError(e -> log.error("WS ERROR", e))
                .doFinally(sig -> log.info("FINALLY={}", sig));


        flux.take(10)
            .blockLast(Duration.ofSeconds(20)); // 20초 안에 10개 못 받으면 타임아웃
    }
}