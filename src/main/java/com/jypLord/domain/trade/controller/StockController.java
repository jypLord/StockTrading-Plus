package com.jypLord.domain.trade.controller;

import com.jypLord.auth.jwt.AuthenticatedUser;
import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final TradeService tradeService;

    @PostMapping("/priceSetting")
    public Mono<ResponseEntity<Void>> registerThresholdPrice(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody RegisterTradeInfoRequest dto
    ) {
        return tradeService.registerTradeInfo(authenticatedUser.id(), dto)
            .thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping(value = "/receive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AssetPrice>> receiveAssetInfo(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam BrokerageFirm firm
    ) {
        return tradeService.receiveAssetInfo(authenticatedUser.id(), firm)
            .map(assetPrice -> ServerSentEvent.<AssetPrice>builder()
                .event("asset-price")
                .data(assetPrice)
                .build());
    }

    @PostMapping("/manage")
    public Mono<ResponseEntity<Void>> manageAsset(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam BrokerageFirm firm
    ) {
        return tradeService.manageAsset(authenticatedUser.id(), firm)
            .then()
            .thenReturn(ResponseEntity.ok().build());
    }
}
