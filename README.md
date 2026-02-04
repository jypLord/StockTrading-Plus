# autoInvest

주식 관리 API입니다.

주식을 하면서 손절을 못하는 이유는, "혹시 여기서 다시 오르면 어떡하지?" 라는 생각 때문입니다.

주가를 실시간 (WebSocket && HTTP Polling) 으로 감시하다가, 지정한 손절가에 도달하면 자동 손절.
이후 감시를 지속하여 주가가 손절가에 다시 도달하면 재매수하게 하는 API입니다.

현재 LS 증권의 세션을 다룰 수 있으며, 다른 증권사를 추가하기 위해 전략패턴(디자인패턴)을 사용하여 원활한 확장성을 제고하였습니다.

---

## 1) 문제 해결 요약
### 메모리 트러블
- **문제 정의**
  - EC2 micro 환경에서 계속 유지되고 있는 유저들의 웹소켓 세션으로 메모리가 포화되어 한 서버에 많은 유저를 감당하기 어려움.
  - 
- **해결 방향**
  - 모든 서버에서 **주식 종목당 단 한 개의 세션만 유지**하고, 전역 Redis로 **브로드캐스팅(Pub/Sub)**
  - 중복 쓰기 및 데이터 정합성을 보장하기 위해 **Redis 분산락** 으로 유일한 쓰기 보장. 

  - 거래/후처리 작업은 **Redis Streams**로 **작업 큐**를 구성해 안정적으로 처리

---
## 2) 기술 스택

**Language**: ![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)

**Framework**: ![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring WebFlux](https://img.shields.io/badge/WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

**DB**: ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![R2DBC](https://img.shields.io/badge/R2DBC-0A7FC1?style=for-the-badge&logo=reactivex&logoColor=white)

**Cache / Messaging**: ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

**Infra**: ![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![EC2](https://img.shields.io/badge/EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![RDS](https://img.shields.io/badge/RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

## 3) 아키텍처
<img width="1512" height="993" alt="image" src="https://github.com/user-attachments/assets/3e089228-8830-4bea-909f-87751f972f66" />


## 4) 트러블 슈팅
### 4-1. 너무 많은 세션과 중복 데이터
**문제**

기존 구조: EC2 단일 노드(앱 + Redis + MySQL)

주가 수신을 위해 WebSocket 연결을 유지하는데, micro 인스턴스에서 stateful 세션 폭증을 견디기 어려움

또한 다수 사용자가 동일 종목을 동시에 감시 → 동일한 시세 데이터를 여러 세션이 중복 수신

**해결**

Redis 서버를 분리하고 전역 Pub/Sub 브로드캐스팅으로 “시세 수신을 1회화”

App 서버는 **종목당 1개**의 수신 파이프라인만 유지하고, 내부/다른 서버로는 Redis로 전파

### 4-2. 순수 메모리 부족
**문제**

기존 상태: MySQL + JVM 기본 옵션 + 단일 EC2

micro 인스턴스에서 DB + App이 같이 돌고 있기 때문에 MySQL InnoDB 버퍼풀 + JVM 힙 + 네이티브/페이지캐시가 경쟁

결국 system.mem.available 급락하여 스왑/GC 폭증으로 이어짐

**해결**

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
