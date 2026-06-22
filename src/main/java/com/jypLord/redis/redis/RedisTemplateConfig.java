package com.jypLord.redis.redis;



import com.jypLord.domain.trade.dto.response.AssetPrice;
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

    @Bean
    public ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate(
        ReactiveRedisConnectionFactory factory
    ) {
        StringRedisSerializer serializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> context =
            RedisSerializationContext.<String, String>newSerializationContext(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

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

