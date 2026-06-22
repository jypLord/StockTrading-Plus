package com.jypLord.apiTest;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.broker.request.price.LsPriceRequest;
import com.jypLord.domain.trade.dto.response.AssetPrice;
import com.jypLord.api.handler.LsBrokerClient;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import reactor.test.StepVerifier;

@Slf4j
@SpringBootTest(properties = {
    "jwt.secret.key=test-secret-test-secret-test-secret-test-secret",
    "DB_USERNAME=test",
    "DB_PASSWORD=test",
    "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
    "spring.kafka.listener.auto-startup=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class LsClientTest {

    @MockBean
    private ReactiveRedisConnectionFactory redisConnectionFactory;

    @Autowired
    LsBrokerClient client;


    String accessToken = System.getenv("LS_ACCESS_TOKEN");


    @Test
    void websocket_price_stream_test() {
        assumeTrue(accessToken != null && !accessToken.isBlank(), "LS_ACCESS_TOKEN is required");

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
        assumeTrue(accessToken != null && !accessToken.isBlank(), "LS_ACCESS_TOKEN is required");

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
