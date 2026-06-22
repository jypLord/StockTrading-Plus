package com.jypLord;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

@SpringBootTest(properties = {
    "jwt.secret.key=test-secret-test-secret-test-secret-test-secret",
    "DB_USERNAME=test",
    "DB_PASSWORD=test",
    "KAFKA_BOOTSTRAP_SERVERS=localhost:9092",
    "spring.kafka.listener.auto-startup=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class AutoInvestApplicationTests {

    @MockBean
    private ReactiveRedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {
    }

}
