package com.jypLord.domain.trade.repository;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.TradeStatus;
import com.jypLord.domain.trade.entity.Trade;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TradeRepository extends R2dbcRepository<Trade, Long> {

    @Query("""
        SELECT *
        FROM trade
        WHERE user_id = :userId
          AND stock_code = :stockCode
          AND trade_status = :status
        LIMIT 1
        """)
    Mono<Trade> findByUserIdAndStockCodeAndStatus(
        @Param("userId") Long userId,
        @Param("stockCode") String stockCode,
        @Param("status") TradeStatus status
    );

    @Query("""
        SELECT *
        FROM trade
        WHERE user_id = :userId
          AND stock_code = :stockCode
          AND trade_status IN (
              'ACTIVE',
              'LOSSCUT_TRIGGERED',
              'LOSSCUT_ORDER_SUBMITTED',
              'EXECUTED_LOSSCUT',
              'REBUY_WATCHING',
              'REBUY_ORDER_SUBMITTED',
              'EXECUTED_BUY'
          )
        LIMIT 1
        """)
    Mono<Trade> findMonitorableTradeByUserIdAndStockCode(
        @Param("userId") Long userId,
        @Param("stockCode") String stockCode
    );

    @Query("""
        SELECT *
        FROM trade
        WHERE user_id = :userId
          AND trade_status = :status
        """)
    Flux<Trade> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TradeStatus status);

    @Query("""
        SELECT *
        FROM trade
        WHERE trade_status = :status
        """)
    Flux<Trade> findByStatus(@Param("status") TradeStatus status);

    @Query("""
        SELECT *
        FROM trade
        WHERE user_id = :userId
          AND trade_status = 'ACTIVE'
          AND firm = :firm
        """)
    Flux<Trade> findValidTradeByUserId(@Param("userId") Long userId, @Param("firm") BrokerageFirm firm);

    @Modifying
    @Query("""
        UPDATE trade
        SET trade_status = :newStatus
        WHERE id = :tradeId
          AND trade_status = :expectedStatus
        """)
    Mono<Boolean> updateTradeStatus(
        @Param("tradeId") Long tradeId,
        @Param("expectedStatus") TradeStatus expectedStatus,
        @Param("newStatus") TradeStatus newStatus
    );

    @Modifying
    @Query("""
        UPDATE trade
        SET trade_status = 'EXPIRED'
        WHERE trade_status = 'ACTIVE'
          AND created_at < NOW() - INTERVAL 1 DAY
        """)
    Mono<Integer> bulkDeactivateOldTrades();
}
