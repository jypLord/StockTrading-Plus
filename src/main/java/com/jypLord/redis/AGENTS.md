## Directory Description
이 디렉토리는 Redis 의 기능을 만들기 위한 공간이다.

## Rule
Redis Pub은 RedisPublisher에, Redis Sub은 Redis Subscriber에 기능을 추가한다.
그리고 비지니스 로직에서 사용할 메서드를 RedisWrapper에 구현하라.

Redis cache는 Redis Wrapper에 직접 만든다.