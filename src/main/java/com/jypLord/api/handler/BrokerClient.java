package com.jypLord.api.handler;

import com.jypLord.api.dto.request.buy.response.BuyResponse;
import com.jypLord.api.dto.request.getPrice.PriceRequest;
import com.jypLord.api.dto.request.sell.response.SellResponse;
import com.jypLord.api.dto.request.stockOAuth.StockOAuthRequest;
import com.jypLord.api.dto.request.buy.request.BuyRequest;
import com.jypLord.api.dto.request.sell.SellRequest;
import com.jypLord.api.dto.response.AssetPrice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BrokerClient {
    public Flux<AssetPrice> receivePrice(PriceRequest dto);
    public Mono<String> getOAuthToken(StockOAuthRequest dto);
    public Mono<SellResponse> sell(SellRequest dto);
    public Mono<BuyResponse> buy(BuyRequest dto);
}
