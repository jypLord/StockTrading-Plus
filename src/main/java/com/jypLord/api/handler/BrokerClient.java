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

public interface BrokerClient {
    public Flux<AssetPrice> receivePrice(PriceRequest dto);
    public Mono<String> getOAuthToken(StockOAuthRequest dto);
    public Mono<SellResponse> sell(SellRequest dto);
    public Mono<BuyResponse> buy(BuyRequest dto);
    public Mono<TradeInfoResponse> getTradeInfo(TradeInfoRequest dto);
}
