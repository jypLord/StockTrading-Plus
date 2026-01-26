package com.jypLord.api.dto.request.connect;

import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class  ConnectRequest {
    Long userId;
    String oAuthToken;
    BrokerageFirm firm;
}
