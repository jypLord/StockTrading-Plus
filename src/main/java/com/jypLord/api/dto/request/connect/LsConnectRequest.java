package com.jypLord.api.dto.request.connect;


import com.jypLord.api.BrokerageFirm;
import lombok.Builder;
import lombok.Getter;

@Getter
public class LsConnectRequest extends ConnectRequest{
    public LsConnectRequest(long userId, String oAuthToken, BrokerageFirm firm) {
        super(userId,  oAuthToken, firm);
    }
}
