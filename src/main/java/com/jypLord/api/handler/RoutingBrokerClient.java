package com.jypLord.api.handler;


import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.broker.request.buy.BuyRequest;
import com.jypLord.api.dto.broker.request.price.PriceRequest;
import com.jypLord.api.dto.broker.request.sell.SellRequest;
import com.jypLord.api.dto.broker.request.stockOAuth.StockOAuthRequest;
import com.jypLord.api.dto.broker.request.tradeInfo.TradeInfoRequest;
import com.jypLord.api.dto.broker.request.tradeInfo.LsTradeInfoRequest;
import com.jypLord.api.dto.broker.response.BuyResponse;
import com.jypLord.api.dto.broker.response.SellResponse;
import com.jypLord.api.dto.broker.response.TradeInfoResponse;
import com.jypLord.domain.trade.dto.response.AssetPrice;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class RoutingBrokerClient implements BrokerClient{

    private final Map<BrokerageFirm, BrokerApiClient> clients = new EnumMap<>(BrokerageFirm.class);

    public RoutingBrokerClient(List<BrokerApiClient> brokerClients) {

        for(BrokerApiClient client : brokerClients){

            clients.put(client.getFirm(), client);
        }

    }

    @Override
    public Flux<AssetPrice> receivePrice(PriceRequest dto) {
        return clients.get(dto.getFirm()).receivePrice(dto);
    }

    @Override
    public Mono<String> getOAuthToken(StockOAuthRequest dto) {
        return clients.get(dto.getFirm()).getOAuthToken(dto);
    }

    @Override
    public Mono<SellResponse> sell(SellRequest dto) {
        return clients.get(dto.getFirm()).sell(dto);
    }

    @Override
    public Mono<BuyResponse> buy(BuyRequest dto) {
        return clients.get(dto.getFirm()).buy(dto);
    }

    @Override
    public Mono<TradeInfoResponse> getTradeInfo(TradeInfoRequest dto) {
        if (dto instanceof LsTradeInfoRequest) {
            return clients.get(BrokerageFirm.LS).getTradeInfo(dto);
        }

        return Mono.error(new IllegalArgumentException("Unsupported trade info request: " + dto.getClass().getName()));
    }
}
