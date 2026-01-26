package com.jypLord.redis.streams.consumer;

import com.jypLord.redis.streams.EventStreamDispatcher;
import com.jypLord.redis.streams.StreamEnvelope;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamReceiver;
import com.jypLord.redis.streams.StreamKey;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;


@Log4j2
@Component
@RequiredArgsConstructor
public class RedisStockEventConsumer {

    private final ReactiveStringRedisTemplate redis;
    private final StreamReceiver<String, MapRecord<String, String, String>> receiver;

    private static final String GROUP = "ctrl-group";
    private final String consumerName = System.getenv().getOrDefault("HOSTNAME", "local");

    private Disposable sub;

    private final EventStreamDispatcher dispatcher;

    @PostConstruct
    public void start() {

        Flux<StreamEnvelope> merged =
            Flux.merge(
                receive(receiver, StreamKey.BROKER_SESSION_TERMINATED_STREAM),
                receive(receiver, StreamKey.LOSSCUT_STREAM)
            );

        this.sub = merged
            .flatMap(this::handleAndAck, 16) // 동시성 16
            .doOnError(e -> log.error("프로세서 에러", e))
            .subscribe();
    }

    private Flux<StreamEnvelope> receive(StreamReceiver<String, MapRecord<String, String, String>> receiver,
        StreamKey streamKey) {


        Consumer consumer = Consumer.from(GROUP, consumerName);

        StreamOffset<String> offset = StreamOffset.create(streamKey.key(), ReadOffset.lastConsumed());

        return receiver.receive(consumer, offset)
            .doOnNext(msg -> log.debug("[{}] id={} body={}", streamKey, msg.getId(), msg.getValue()))
            .map(msg -> new StreamEnvelope(streamKey, msg));
    }

    private Mono<Void> handleAndAck(StreamEnvelope env) {
        StreamKey streamKey = env.streamKey();

        var rec = env.record();

        return dispatcher.dispatch(env)

            .then(ack(streamKey, rec.getId()))

            .onErrorResume(e -> {
                log.error("[{}] 실패 id={} 에러={}", streamKey, rec.getId(), e.toString(), e);
                return Mono.empty();
            });
    }

    private Mono<Void> ack(StreamKey streamKey, RecordId id) {
        return redis.opsForStream()
            .acknowledge(streamKey.key() , GROUP, id) // Mono<Long>
            .then();
    }

    @PreDestroy
    public void stop() {
        if (sub != null) sub.dispose();
    }

}