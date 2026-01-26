package com.jypLord.config.redis;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptions;

@Configuration
public class RedisStreamsConfig {

    @Bean
    public StreamReceiverOptions<String, MapRecord<String, String, String>> redisStreamReceiverOptions() {
        return StreamReceiverOptions.builder()
            .pollTimeout(Duration.ofSeconds(1))
            .build();
    }

    @Bean
    public StreamReceiver<String, MapRecord<String, String, String>> streamReceiver(
        ReactiveRedisConnectionFactory factory,
        StreamReceiverOptions<String, MapRecord<String, String, String>> redisStreamReceiverOptions
    ) {
        return StreamReceiver.create(factory, redisStreamReceiverOptions);
    }
}