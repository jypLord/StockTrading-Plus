package com.jypLord.api.handler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.broker.request.buy.BuyRequest;
import com.jypLord.api.dto.broker.request.price.PriceRequest;
import com.jypLord.api.dto.broker.request.sell.SellRequest;
import com.jypLord.api.dto.broker.request.stockOAuth.StockOAuthRequest;
import com.jypLord.api.dto.broker.request.tradeInfo.TradeInfoRequest;
import com.jypLord.api.dto.broker.response.BuyResponse;
import com.jypLord.api.dto.broker.response.SellResponse;
import com.jypLord.api.dto.broker.response.TradeInfoResponse;
import com.jypLord.api.handler.BrokerApiClient;
import com.jypLord.api.handler.BrokerClient;
import com.jypLord.domain.trade.dto.response.AssetPrice;
import com.jypLord.util.JsonUtil;
import io.netty.handler.logging.LogLevel;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.net.URI;
import java.time.Duration;

@Log4j2
@Component
public class BinanceBrokerClient implements BrokerApiClient {


    @Override
    public Flux<AssetPrice> receivePrice(PriceRequest dto) {
        return receivePriceForWebsocket();
    }

    @Override
    public Mono<String> getOAuthToken(StockOAuthRequest dto) {
        return null;
    }

    @Override
    public Mono<SellResponse> sell(SellRequest dto) {
        return null;
    }

    @Override
    public Mono<BuyResponse> buy(BuyRequest dto) {
        return null;
    }

    @Override
    public Mono<TradeInfoResponse> getTradeInfo(TradeInfoRequest dto) {
        return null;
    }


    public Flux<AssetPrice> receivePriceForWebsocket(
            Long userId, String accessToken, String stockCode, Sinks.Many<String> outbound){

        HttpClient httpClient = HttpClient.create()
                .wiretap(
                        "reactor.netty.http.client.HttpClient",
                        LogLevel.DEBUG,
                        AdvancedByteBufFormat.HEX_DUMP
                );

        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient(httpClient);

        return Flux.create(emitter -> {

            // 메모리 정리를 위해 disposable로 나눔
            Disposable disposable = client.execute(
                            URI.create("wss://ws-fapi.binance.com/ws-fapi/v1"),

                            session -> {

                                Mono<Void> send = session.send(
                                        outbound.asFlux()
                                                .map(session::textMessage)

                                                .doOnNext(log::debug)
                                ).then();


                                Mono<Void> receive = session.receive()
                                        .map(WebSocketMessage::getPayloadAsText)
                                        .map(text -> {

                                            JsonNode root = JsonUtil.toJsonNode(text);
                                            JsonNode rootBody = root.path("body");

                                            String code = rootBody.path("shcode").asText();
                                            int priceNow = rootBody.path("bidho").asInt();

                                            return new AssetPrice(code, priceNow, userId, BrokerageFirm.LS);
                                        })
                                        .doOnNext(a->log.debug(a.stockCode() + ":"+ a.price()))
                                        .doOnNext(emitter::next)

                                        .timeout(Duration.ofSeconds(10))
                                        .then();


                                return Mono.when(send, receive);
                            })
                    .doOnError(emitter::error)
                    .doOnTerminate(emitter::complete)
                    .subscribe();


            emitter.onDispose(disposable);
            emitter.onCancel(disposable);

        });
    }

    @Override
    public BrokerageFirm getFirm() {
        return BrokerageFirm.BINANCE;
    }
}
