package com.jypLord.exception.broker;

public class KoreanMarketOverTimeException extends BrokerException {

    public KoreanMarketOverTimeException() {
        super("한국 장 종료. 실시간 주가 수신 불가능");
    }
}
