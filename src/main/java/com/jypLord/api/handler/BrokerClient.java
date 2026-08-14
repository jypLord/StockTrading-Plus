package com.jypLord.api.handler;

import com.jypLord.api.dto.broker.request.buy.BuyRequest;
import com.jypLord.api.dto.broker.request.price.PriceRequest;
import com.jypLord.api.dto.broker.request.sell.SellRequest;
import com.jypLord.api.dto.broker.request.stockOAuth.StockOAuthRequest;
import com.jypLord.api.dto.broker.request.tradeInfo.TradeInfoRequest;
import com.jypLord.api.dto.broker.response.BuyResponse;
import com.jypLord.api.dto.broker.response.SellResponse;
import com.jypLord.api.dto.broker.response.TradeInfoResponse;
import com.jypLord.domain.trade.dto.response.AssetPrice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

public interface BrokerClient {

    ConcurrentHashMap<Long, Sinks.Many<String>> webSocketSinks = new ConcurrentHashMap<>();

    Flux<AssetPrice> receivePrice(PriceRequest dto);
    Mono<String> getOAuthToken(StockOAuthRequest dto);
    Mono<SellResponse> sell(SellRequest dto);
    Mono<BuyResponse> buy(BuyRequest dto);
    Mono<TradeInfoResponse> getTradeInfo(TradeInfoRequest dto);
}
