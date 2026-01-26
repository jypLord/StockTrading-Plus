package com.jypLord.config.r2dbc;

import com.jypLord.domain.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

@Log4j2
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class TradeSchedulerConfig {

    private final TradeRepository tradeRepository;

    /**
     * 매일 새벽 4시(Asia/Seoul)마다 일괄 업데이트
     */
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    public void updateTradesAt4AM() {

        Mono<Integer> updateMono = tradeRepository.bulkDeactivateOldTrades();

        updateMono
            .doOnSubscribe(sub -> log.info("[TradeScheduler] 4시 일괄 업데이트 시작"))
            .doOnNext(count -> log.info("[TradeScheduler] 업데이트된 행 수: {}", count))
            .doOnError(e -> log.error("[TradeScheduler] 일괄 업데이트 실패", e))
            .subscribe();
    }
}