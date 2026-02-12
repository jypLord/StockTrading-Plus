package com.jypLord.redis.streams.consumer.handler;

import com.jypLord.redis.streams.StreamEnvelope;
import com.jypLord.redis.streams.StreamKey;
import reactor.core.publisher.Mono;


public interface StreamEventHandler {
    StreamKey getKey();
    Mono<Void> handle(StreamEnvelope env);
}
