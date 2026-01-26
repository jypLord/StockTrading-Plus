package com.jypLord.api.dto.response;


import com.jypLord.api.BrokerageFirm;

public record AssetPrice(
    String stockCode, int price, Long sourceUserId, BrokerageFirm sourceBroker
) {}
