package com.jypLord.api.handler;

import com.jypLord.exception.broker.KoreanMarketOverTimeException;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.logging.LogLevel;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.getPrice.PriceRequest;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.exception.broker.FailRetrievingStockInfoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jypLord.api.dto.request.buy.response.BuyResponse;
import com.jypLord.api.dto.request.buy.request.BuyRequest;
import com.jypLord.api.dto.request.sell.SellRequest;
import com.jypLord.api.dto.request.sell.response.SellResponse;
import com.jypLord.api.dto.request.stockOAuth.LsStockOAuthRequest;
import com.jypLord.api.dto.request.stockOAuth.StockOAuthRequest;
import com.jypLord.exception.broker.StockOAuthException;
import com.jypLord.util.JsonUtil;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;


@Log4j2
@Component
@RequiredArgsConstructor
public class LsBrokerClient implements BrokerClient{

    private final WebClient.Builder client;

    private final ConcurrentHashMap<Long, Sinks.Many<String>> webSocketSinks = new ConcurrentHashMap<>();

    @Override
    public Flux<AssetPrice> receivePrice(PriceRequest dto) {

        if (!LocalTime.now().isBefore(LocalTime.of(15, 30))) {
            return Flux.error(new KoreanMarketOverTimeException());
        }


        return emitPriceRequest(dto.getUserId(), dto.getStockCodes(), dto.getStockAccessToken())
            .flatMapMany(sink -> receivePriceForWebsocket(dto.getUserId(), dto.getStockAccessToken(), dto.getStockCodes(), sink))
            .onErrorResume(TimeoutException.class, e -> {
                log.debug("LS 증권 Websocket 타임아웃. userId={}, stockCode={}",
                    dto.getUserId(), dto.getStockCodes());
                return receivePriceViaPolling(dto.getUserId(), dto.getStockAccessToken(), dto.getStockCodes())
                    .doOnSubscribe(s-> log.info("웹소켓 세션 이상으로 Polling 시작. stockCode= {}", dto.getStockCodes()));
        });
    }

    public Flux<AssetPrice> receivePriceViaPolling(
        Long userId, String accessToken, String stockCode){

        client.baseUrl("https://openapi.ls-sec.co.kr:8080")
            .defaultHeaders(headers -> {
                headers.set("content-type", "application/json; charset=utf-8 ");
                headers.set("tr_cd", "t1102");
                headers.set("tr_cont", "N");
                headers.set("tr_cont_key", "");
            });

        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
            .map(a -> {

                Map<String, String> inBlock = new HashMap<>();

                inBlock.put("shcode", stockCode);

                Map<String, Object>  outBlock = new LinkedHashMap<>();
                outBlock.put("t1102InBlock", inBlock);

                return JsonUtil.toJson(outBlock);
            })
            .flatMap(JSON ->

                client.build()
                    .post()
                    .uri(uri -> uri.path("/stock/market-data").build())
                    .header("authorization", "Bearer " + accessToken)
                    .bodyValue(JSON)
                    .retrieve()

                    // 응답
                    .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                            .flatMap(body ->
                                Mono.error(new FailRetrievingStockInfoException("주가 Polling 실패: " + body))
                            )
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .map(json->{

                        JsonNode root = JsonUtil.toJsonNode(json);


                        int priceNow =  root.path("t1102OutBlock").path("price").asInt();

                        return new AssetPrice(stockCode, priceNow, userId, BrokerageFirm.LS);
                    })
            );
    }

    private Mono<Sinks.Many<String>> emitPriceRequest(Long userId, String stockCode, String accessToken) {
        return Mono.defer(() -> {
            Sinks.Many<String> sink = webSocketSinks.computeIfAbsent(userId,
                k -> Sinks.many().multicast().onBackpressureBuffer());

            // payload 생성
            Map<String, String> wsRequestHeader = new HashMap<>();
            wsRequestHeader.put("token", accessToken);
            wsRequestHeader.put("tr_type", "3");

            Map<String, String> body = new HashMap<>();
            body.put("tr_cd", "S2_");
            body.put("tr_key", stockCode);

            Map<String, Object> outBlock = new HashMap<>();
            outBlock.put("header", wsRequestHeader);
            outBlock.put("body", body);

            String payload = JsonUtil.toJson(outBlock);

            Sinks.EmitResult result = sink.tryEmitNext(payload);
            if (result.isFailure()) log.warn("emit failed userId={} result={}", userId, result);

            return Mono.just(sink);
        });
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
                    URI.create("wss://openapi.ls-sec.co.kr:29443/websocket"),

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

    /*
    LS 증권 접근토큰 발급
    */
    @Override
    public Mono<String> getOAuthToken(StockOAuthRequest dto) {

        LsStockOAuthRequest downCast = (LsStockOAuthRequest) dto ;

        String appKey = downCast.getAppKey();
        String appSecretKey = downCast.getAppSecretKey();



        return client.baseUrl("https://openapi.ls-sec.co.kr:8080")
            .build()

            .post()
            .uri(uri -> uri.path("/oauth2/token").build())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                .with("appkey", appKey)
                .with("appsecretkey", appSecretKey)
                .with("scope", "oob"))
            .retrieve()

            .onStatus(HttpStatusCode::isError, resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body ->
                        Mono.error(new StockOAuthException("OAuth token error from LS: " + body)))
            )
            .bodyToMono(String.class)
            .doOnSuccess(token -> log.info("LS access token retrieved: {}", dto.getUserId()))
            .map(dirtyToken -> {
                JsonNode jsonNode = JsonUtil.toJsonNode(dirtyToken);

                return jsonNode.get("access_token").asText();
            })
            .timeout(Duration.ofSeconds(5));
    }


    @Override
    public Mono<SellResponse> sell(SellRequest dto) {

        Map<String, Object> inBlock = new LinkedHashMap<>();
        inBlock.put("IsuNo", dto.getStockCode());
        inBlock.put("OrdQty", dto.getQuantity());
        inBlock.put("OrdPrc", dto.getPrice());
        inBlock.put("BnsTpCode", "1");
        inBlock.put("OrdprcPtnCode", "00");
        inBlock.put("MgntrnCode", "000");
        inBlock.put("LoanDt", "");
        inBlock.put("OrdCndiTpCode", "0");
        inBlock.put("MbrNo", "NXT");

        Map<String, Object> body = Map.of("CSPAT00601InBlock1", inBlock);


        return client
            .baseUrl("https://openapi.ls-sec.co.kr:8080")
            .defaultHeaders(headers -> {
                headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8 ");
                headers.setBearerAuth(dto.getOAuthToken());
                headers.set("tr_cd","CSPAT00601"); // �ŷ� CD�ڵ�
                headers.set("tr_cont", "N");
                headers.set("tr_cont_key", "");
                headers.set("mac_address", "");
            })
            .build()

            .post()
            .uri(uri -> uri.path("/stock/order").build())
            .bodyValue(body)
            .retrieve()
            .bodyToMono(SellResponse.class);
    }


    @Override
    public Mono<BuyResponse> buy(BuyRequest dto) {


        Map<String, Object> inBlock = new LinkedHashMap<>();
        inBlock.put("IsuNo", dto.getStockCode());      // 종목코드
        inBlock.put("OrdQty", dto.getQuantity());    // 수량
        inBlock.put("OrdPrc", dto.getPrice());    // 가격
        inBlock.put("BnsTpCode", "2");             //
        inBlock.put("OrdprcPtnCode", "00");        //
        inBlock.put("MgntrnCode", "000");          //
        inBlock.put("LoanDt", "");                 //
        inBlock.put("OrdCndiTpCode", "0");         //
        inBlock.put("MbrNo", "NXT");               //

        Map<String, Object> body = Map.of("CSPAT00601InBlock1", inBlock); // LS ���� �䱸�ϴ� ����


        return client
            .baseUrl("https://openapi.ls-sec.co.kr:8080")
            .defaultHeaders(headers -> {
                headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8 ");
                headers.setBearerAuth(dto.getOAuthToken());
                headers.set("tr_cd","CSPAT00601");
                headers.set("tr_cont", "N");
                headers.set("tr_cont_key", "");
                headers.set("mac_address", "");
            })
            .build()

            .post()
            .uri(uri -> uri.path("/stock/order").build())
            .bodyValue(body)
            .retrieve()
            .bodyToMono(BuyResponse.class);
    }
}
