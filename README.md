# autoInvest

주가를 **실시간 감시**하다가  
① 지정가 도달 → **시장가 손절**  
② 이후 다시 지정가 도달 → **재매수**  
까지 자동으로 실행하는 API 서버입니다

---

## 1) 프로젝트 개요

- **프로젝트명**: autoInvest
- **문제 정의**
  - 실시간 주가 감시는 연결 유지(WebSocket) + 이벤트 폭주 특성이 강해서
  - 작은 인스턴스(EC2 micro)에서는 **세션과 메모리중복 데이터**가 바로 병목으로 다가옴
- **해결 방향**
  - 실시간 주가 수신은 **한 번만** 받아서 전역 Redis로 **브로드캐스팅(Pub/Sub)**
  - 거래/후처리 작업은 **Redis Streams**로 **작업 큐**를 구성해 안정적으로 처리

---

## 2) 아키텍처

> 환경: **EC2 micro** 기반  
> 구성: **App 서버 + 전역 Redis 서버 + RDS(MySQL)**

## 3) 기술 스택

Language: Java 17

Framework: Spring Boot (WebFlux)

DB: MySQL (AWS RDS)

Cache / Messaging: Redis

Pub/Sub: 실시간 가격 브로드캐스팅

Streams: 작업 큐 (Consumer Group)

Infra: AWS EC2 micro, RDS


## 4) 트러블 슈팅
### 4-1. 너무 많은 세션과 중복 데이터
문제

기존 구조: EC2 단일 노드(앱 + Redis + MySQL)

주가 수신을 위해 WebSocket 연결을 유지하는데, micro 인스턴스에서 stateful 세션 폭증을 견디기 어려움

또한 다수 사용자가 동일 종목을 동시에 감시 → 동일한 시세 데이터를 여러 세션이 중복 수신

해결

Redis 서버를 분리하고 전역 Pub/Sub 브로드캐스팅으로 “시세 수신을 1회화”

App 서버는 **종목당 1개**의 수신 파이프라인만 유지하고, 내부/다른 서버로는 Redis로 전파

### 4-2. 순수 메모리 부족
문제

기존 상태: MySQL + JVM 기본 옵션 + 단일 EC2

micro 인스턴스에서 DB + App이 같이 돌고 있기 때문에 MySQL InnoDB 버퍼풀 + JVM 힙 + 네이티브/페이지캐시가 경쟁

결국 system.mem.available 급락하여 스왑/GC 폭증으로 이어짐

해결

1. DB를 RDS로 분리: App 서버에서 MySQL 메모리 풋프린트를 제거

2. InnoDB Buffer Pool 조정:
읽기는 단 한번, 쓰기는 없을 수 있는 API 특성상 DB 캐시 효용이 낮음.
따라서 버퍼풀을 과감히 낮춰 메모리 여유 확보

JVM 힙 상한 설정: micro에서 힙이 OS 메모리를 잠식하지 않게 고정
```
JAVA_OPTS="
  -Xms256m -Xmx384m
  -XX:MaxMetaspaceSize=128m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/autoInvest/heapdump.hprof
  -Dfile.encoding=UTF-8
"
```

```
[mysqld]
innodb_buffer_pool_size=128M
innodb_buffer_pool_instances=1
max_connections=100
```
