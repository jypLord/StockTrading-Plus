# StockTrading- Plus

> 증권사에서 제공하지 않는 기능을 추가한 미니 증권사 프로젝트입니다.


## 프로젝트 소개

사용자가 설정한 기준가를 토대로 실시간 시세를 감시하고,  
조건 도달 시 자동 손절하고, 다시 기준가까지 주가가 회복하면 재매수하는 주식 관리 API입니다.

**실시간 데이터 수신**, **중복 처리 방지**, **서버 간 데이터 공유**, **비동기 후처리 안정성**까지 고려해 설계한 프로젝트입니다.
실시간 연결이 많은 환경에서 발생하는 **세션 폭증, 메모리 부족, 중복 처리, 커넥션 풀 병목** 문제를 해결하는 과정까지 담았습니다.

현재는 **LS증권 연동**을 지원하며,  
추후 다른 증권사들을 쉽게 추가할 수 있도록 **전략 패턴**를 적용했습니다.

---


## 핵심 기능

- 사용자 인증 및 인가
- 관심 종목 감시 등록
- 실시간 시세 수신
- 손절가 도달 시 자동 매도
- 재진입 조건 충족 시 자동 매수
- 거래 이력 조회
- 서버 간 실시간 가격 데이터 공유
- 중복 거래 방지 및 후처리 비동기화

---

## 기술 스택

- Language: Java 17
- Framework: Spring Boot, Spring WebFlux, Spring Security
- Database: MySQL, R2DBC
- Cache / Messaging: Redis Pub/Sub, Apache Kafka
- Infra: AWS EC2, RDS, Elastic Cache(Redis), MSK ,Docker,GitHub Actions
- Monitoring: AWS CloudWatch, 메트릭 기반 성능 분석

---

## 아키텍처
![autoInvest_최종](https://github.com/user-attachments/assets/6a948b89-c955-4d75-8758-8dfd851d3c1d)


### 구조 요약
- 외부 증권사로부터 실시간 시세 수신
- 동일 종목은 서버마다 개별 수신하지 않고 공유
- Redis Pub/Sub으로 가격 데이터를 각 App 서버에 브로드캐스팅
- 거래/후처리는 Redis Streams 기반으로 분리
- 분산 환경에서 중복 실행 방지를 위해 Redis 기반 락 적용
<img width="5605" height="6125" alt="Untitled diagram-2026-02-11-051546" src="https://github.com/user-attachments/assets/101e3c04-156b-4b0e-a9e6-e522105f152c" />

---

## 기술 선택 이유

### 1. Spring WebFlux
2v CPU를 가지고있는 EC2 t3.micro 환경에서 많은 클라이언트 연결과 WebSocket 세션을 유지해야 하기 때문에 Blocking 모델에는 한계가 있었습니다.
적은 자원으로 더 많은 연결을 처리하고 I/O 효율을 극대화 하기 위해 WebFlux를 선택했습니다.

### 2. Redis Pub/Sub
많은 scale-out 서버를 대상으로 하나의 데이터 소스에서 실시간 데이터를 Broadcasting 하기 위해 선택했습니다.

### 3. Redis Streams
실시간 데이터 브로드캐스팅은 API의 기둥이기 때문에, 예비 서버들을 두어야 했습니다. 
그 서버에 Replica와 Sentinel을 구성하고 RDB와 AOF 를 통해 영속성 및 가용성 을 확보하여 MQ를 Redis Streams로 사용하는 것으로 결정하였습니다.

기술 목표가 '최대한 적은 비용으로 최대 출력' 이기 때문에, Kafka 사용 등 추가 서버 노드를 발생시키지 않는 방법을 선택했습니다.

---

## 트러블슈팅 및 성능 개선
### 1. 실시간 주가 수신 구조 개선

문제: 400명 이상 동시 수신 시 CPU 사용률 90% 초과, 에러율 급증

원인: 클라이언트마다 외부 WebSocket 연결을 개별 생성해 동일 종목도 중복 수신

해결: 종목당 1개의 수신 파이프라인만 유지하고, Redis Pub/Sub으로 모든 서버에 가격 데이터 브로드캐스팅

결과: 한 서버 기준 동시 클라이언트 수 400 → 3000 수준으로 개선, 운영 기준은 1500 동시 접속

### 2. Redis Pub/Sub Stampede

문제: 실시간 데이터의 source 세션 종료 시 해당 종목을 수신받던 사용자들이 일제히 증권사 API 호출을 하는 Cache Stampede와 같은 현상 발생

원인: 세션 복구 과정에서 동일 종목에 대한 동시성 제어 부재

해결: Redis Lua 스크립트 기반 분산 락 적용으로 최초 1회만 실제 재구독 수행

결과: stampede 방지, 주가가 최초로 Redis에 publish 되기 전의 중복 요청 방지

### 3. 주문 이벤트 중복 처리 방지

문제: 손절·재매수 조건이 짧은 시간 안에 여러 번 감지되며 주문이 중복 실행됨

해결: PK 기반 멱등 키 생성 후, trade_status를 조건부 UPDATE하여 상태 변경이 성공한 경우에만 실제 주문 수행

결과: 중복 감지, 재시도, 장애 복구 상황에서도 중복 주문 방지

### 4. 애플리케이션 커넥션 풀 병목 해소

문제: K6 10,000 동시 접속 SELECT 테스트에서 p95 450ms, R2DBC pending acquire 급증

원인 : max_connections 을 꽉채운 상태에서 CPU 사용률이 40% 대를 유지하는 것을 확인하고, 애플리케이션 커넥션 풀이 병목으로 작용하고 있음을 확인.

해결: R2DBC pool 10 → 20, MySQL max_connections 85 → 170 조정

결과: p95 450ms → 150ms로 개선

---
