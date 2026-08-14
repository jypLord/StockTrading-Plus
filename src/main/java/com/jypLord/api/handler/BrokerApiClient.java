package com.jypLord.api.handler;

import com.jypLord.api.BrokerageFirm;

/*
실제 증권 거래소들의 API Handler 구현체를 위한 인터페이스. 상위 인터페이스와 시그니처가 같으나, 서비스는 하나의 인터페이스만 바라보고 구현체 라우팅은 다른 클래스에서 하기 위해 만듬.
 */
public interface BrokerApiClient extends BrokerClient{

    BrokerageFirm getFirm();
}
