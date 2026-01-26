package com.jypLord.redis.pub;

import com.jypLord.api.dto.response.AssetPrice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Log4j2
@Component
@RequiredArgsConstructor
public class RedisAssetPricePublisher {


    private final ReactiveRedisTemplate<String, String> redis;

    private final String serverId = System.getenv().getOrDefault("HOSTNAME", "local");

    public Mono<Boolean> acquireLockIfAbsent(String stockCode, Long userId){

        String ACQUIRE_LUA = """
            if redis.call('EXISTS', KEYS[1]) == 0 then
                redis.call('SET', KEYS[1], ARGV[1])
                redis.call('EXPIRE', KEYS[1], ARGV[2])
                return 1
            else return 0
            end
            """;

        RedisScript<Boolean> script = RedisScript.of(ACQUIRE_LUA, Boolean.class);

        String lockKey =  "lock:stock:" + stockCode;

        String LOCK_TTL = "5";

        return redis.execute(script,List.of(lockKey), serverId +":"+ userId.toString(), LOCK_TTL)
            .single();
    }
    /*
    * 분산락 소유자인지 확인하고 값 publish
    * */
    public Mono<Void> publishIfLockOwner(AssetPrice assetPrice) {

        String channel = "stock:price:" + assetPrice.stockCode();

        String lockKey = "lock:stock" + ":" + assetPrice.stockCode();
        String lockValue =  serverId +":"+ assetPrice.sourceUserId();

        String LOCK_TTL = "5";
        String stockPrice = String.valueOf(assetPrice.price());

        String PUBLISH_LUA = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            
                redis.call('PUBLISH', KEYS[2], ARGV[3])
                return 1
            else return 0
            end
            """;

        RedisScript<Boolean> script = RedisScript.of(PUBLISH_LUA, Boolean.class);

        return redis.execute(script, List.of(lockKey, channel), lockValue,  LOCK_TTL , stockPrice)
            .single()
            .then();
    }
    public Mono<Void> removeLockIfOwner(Long userId, String stockCode) {

        String DELETE_LOCK_LUA = """
            if redis.call('GET', KEYS[1]) ==  ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            else
                return 0
            end
            """;

        RedisScript<Boolean> script = RedisScript.of(DELETE_LOCK_LUA, Boolean.class);

        String lockKey = "lock:stock" + ":" + stockCode;

        return redis.execute(script, List.of(lockKey), serverId + ":" + userId)
            .single()
            .then();
    }

}
