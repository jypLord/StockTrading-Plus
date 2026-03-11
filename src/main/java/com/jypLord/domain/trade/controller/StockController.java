package com.jypLord.domain.trade.controller;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.handler.LsBrokerClient;
import com.jypLord.domain.trade.dto.request.RegisterTradeInfoRequest;
import com.jypLord.domain.trade.repository.TradeRepository;
import com.jypLord.domain.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final TradeRepository tradeRepository;
    private final TradeService tradeService;
    private final LsBrokerClient client;
    // 손절가 등록
    @PostMapping("/priceSetting")
    public Mono<ResponseEntity<Void>> registerThresholdPrice(@Valid @RequestBody RegisterTradeInfoRequest dto) {
        return tradeService.registerTradeInfo(dto)
            .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()));
    }


    @PostMapping("/manage")
    public Mono<ResponseEntity<Void>> manageAsset(@RequestParam Long userId, @RequestParam
       BrokerageFirm firm) {

        return tradeService.manageAsset(userId, firm)
            .then(Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).build()));

   }

}
