package com.jypLord.domain.trade.repository;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.entity.Trade;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TradeRepository extends R2dbcRepository<Trade, Long> {


    @Query("""
    SELECT *
    FROM trade t JOIN users u ON t.user_id = u.id
    WHERE t.user_id = ?1 AND t.stock_code = ?2 AND t.trade_status = ?3
    """)
    public Mono<Trade> findByUserIdAndStockCodeAndStatus(Long userId, String stockCode, TradeStatus status);

    @Query("""
    SELECT *
    FROM trade t JOIN users u ON t.userId = u.id
    WHERE t.user_id = ?1 AND t.status = ?2
    """)
    public Flux<Trade> findByUserIdAndStatus(Long userId, TradeStatus status);

    @Query("""
    SELECT *
    FROM trade
    WHERE user_id = :userId AND trade_status = 'ACTIVE' AND firm = :firm
    """)
    public Flux<Trade> findValidTradeByUserId(Long userId, BrokerageFirm firm);

    public Mono<Boolean> existsByUserIdAndStockCodeAndTradeStatus(Long id, String stockCode, TradeStatus status);


    @Query("""
        UPDATE trade
        SET trade_status = 'EXPIRED'
        WHERE trade_status = 'ACTIVE'
          AND created_at < NOW() - INTERVAL 1 DAY
        """)
    public Mono<Integer> bulkDeactivateOldTrades();
}
