## 디렉토리 개요
이 디렉토리는 kafka를 관리하는 디렉토리이다
Consumer과 Producer 클래스들만 존재하고, 다른 workflow 관리 클래스를 만들지 않는다.
Consumer는 TradeService.class 에서 핵심 로직을 호출해야하며, 핵심 비지니스 로직을 직접 작성하지 않는다.

## 이벤트 목록
| 구분 | Topic | EventType | Key               | 의미|
|---|---|---|-------------------|---|
| 거래 이벤트 | `trade.events` | `TRADE_EVENT_OCCURRED` | `trade:userId`    | 거래 관련 이벤트가 발생함|
| 세션 종료 | `session.events` | `TRADING_SESSION_CLOSED` | `broker:stockCode` | 특정 증권사와 종목의 거래 세션이 종료됨 |


### Event 개발 방향

1. 거래 이벤트
    아래의 필드를 포함해야한다.
   - 필드
     - 멱등키
     - userId
     - stockCode
     - 거래 타입( SELL, BUY)
   - 컨슈머
     - 알림(이메일)
     - 거래 관리 
       - 거래 타입이 Sell이면 재매수 감시 로직을 실행 Buy면 손절 감시 로직을 실행.
        - 이 컨슈머를 만들 때는 TradeService.class에서 losscutmonitoring()과 rebuymonitorting() 에서 조건이 완료되면 매수 혹은 매도를 한 후 알맞은 event를 발송해야한다
       

2. 세션 종료
   - 필드
     - 멱등키
     - sourceUserId
     - broker 
     - stockCode

