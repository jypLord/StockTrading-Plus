package com.jypLord.domain.trade.controller;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.api.dto.request.getPrice.LsPriceRequest;
import com.jypLord.api.dto.response.AssetPrice;
import com.jypLord.api.handler.LsBrokerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@Log4j2
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class TestController {

    private final LsBrokerClient lsBrokerClient;

    @PostMapping("test")
    public Flux<AssetPrice> test() {

        String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6IjQwYjVhNTZiLWJkZTEtNGRmYy04ZmZiLTA4ZjM4MzE0ZTgxMiIsIm5iZiI6MTc2Mzk2NjU4OSwiZ3JhbnRfdHlwZSI6IkNsaWVudCIsImlzcyI6InVub2d3IiwiZXhwIjoxNzY0MDIxNTk5LCJpYXQiOjE3NjM5NjY1ODksImp0aSI6IlBTUTJrekFMM0d5RkRieXE3RGtTUExBeDZ3Z3c3TUJKREtiayJ9.8NN1ZHvl2CUrcilz3jvzXmA4hkV_dAo88eShWCuwlsYL3My81OkPp0kzTtIJIOZPOIL9yIhn6t6S1XWpphSG3Q";
        return lsBrokerClient.receivePrice(new LsPriceRequest(1L, BrokerageFirm.LS,accessToken, "086520"));
    }
}
