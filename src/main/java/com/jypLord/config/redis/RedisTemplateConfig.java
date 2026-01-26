package com.jypLord.config.redis;



import com.jypLord.api.dto.response.AssetPrice;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean(name = "AssetPrice")
    public ReactiveRedisTemplate<String, AssetPrice> assetPriceRedisTemplate(
        ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<AssetPrice> valueSerializer =
            new Jackson2JsonRedisSerializer<>(AssetPrice.class);

        RedisSerializationContext<String, AssetPrice> context =
            RedisSerializationContext.<String, AssetPrice>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }



    @Bean(name = "<String, Integer>")
    public ReactiveRedisTemplate<String, Integer> stockPriceRedisTemplate(
        ReactiveRedisConnectionFactory factory
    ) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericToStringSerializer<Integer> valueSerializer = new GenericToStringSerializer<>(Integer.class);

        RedisSerializationContext<String, Integer> context =
            RedisSerializationContext.<String, Integer>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisMessageListenerContainer redisMessageListenerContainer(
        ReactiveRedisConnectionFactory factory
    ) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }
}

