package com.jypLord.domain.trade;

public enum TradeStatus {

    ACTIVE,            // 현재 유효한 상태
    EXECUTED_BUY,     // 매수 체결 완료
    EXECUTED_LOSSCUT, // 매도 체결 완료
    CANCELLED,        // 사용자가 주문 취소
    EXPIRED           // 장시간 종료로 만료
}