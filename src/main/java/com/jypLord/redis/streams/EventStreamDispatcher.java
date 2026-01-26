package com.jypLord.redis.streams;

import com.jypLord.redis.streams.handler.StreamEventHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class EventStreamDispatcher {

    private final Map<StreamKey, StreamEventHandler> routes;

    public EventStreamDispatcher(List<StreamEventHandler> handlers) {
        EnumMap<StreamKey, StreamEventHandler> map = new EnumMap<>(StreamKey.class);

        for (StreamEventHandler handler : handlers) {

            StreamEventHandler h = map.putIfAbsent(handler.getKey(), handler);
            if (h != null) throw new IllegalStateException(" 같은 키를 가진 핸들러가 있음 =" + handler.getKey());
        }

        this.routes = Map.copyOf(map);
    }

    public Mono<Void> dispatch(StreamEnvelope env) {
        StreamEventHandler handler = routes.get(env.streamKey());

        if (handler == null) {

            return Mono.error(new IllegalArgumentException(env.streamKey() + " 의 핸들러가 없음"));

        }
        return handler.handle(env);
    }
}