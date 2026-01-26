package com.jypLord.api.dto.request;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class LsStockObserveRequest implements StockMarketRequest {

    @NonNull
    private Long userId;

    @NonNull
    private BrokerageFirm firm;

}
