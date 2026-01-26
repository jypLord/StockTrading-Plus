package com.jypLord.domain.trade.entity;

import com.jypLord.api.BrokerageFirm;
import com.jypLord.domain.trade.TradeStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

@Table("trade")
@Getter
public class Trade {
    @Id
    private Long id;

    @NonNull
    @Column("user_id")
    private final Long userId;

    @NonNull
    @Column("stock_code")
    private final String stockCode;

    @NonNull
    @Column("user_set_price")
    private final Integer userSetPrice;

    @NonNull
    private final Integer quantity;

    @NonNull
    private BrokerageFirm firm;

    @Nullable
    @Column("executed_price")
    private Integer executedPrice;

    @Column("trade_status")
    private TradeStatus tradeStatus;

    // createAt 어노테이션
    @Column("created_at")
    private LocalDateTime createdAt;

    public Trade(Long userId, String stockCode, BrokerageFirm firm, int userSetPrice, int quantity,
        TradeStatus status) {
        this.userId = userId;
        this.stockCode = stockCode;
        this.firm = firm;
        this.userSetPrice = userSetPrice;
        this.quantity = quantity;
        this.tradeStatus = status;

    }

    public Trade(Long userId, String stockCode, int userSetPrice, int quantity,TradeStatus status) {
        this.userId = userId;
        this.stockCode = stockCode;
        this.userSetPrice = userSetPrice;
        this.quantity = quantity;
        this.tradeStatus = status;
    }

    @PersistenceCreator
    public Trade(Long id, Long userId, String stockCode, int userSetPrice, int quantity, TradeStatus tradeStatus, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.stockCode = stockCode;
        this.userSetPrice = userSetPrice;
        this.quantity = quantity;
        this.tradeStatus = tradeStatus;
        this.createdAt = createdAt;
    }


    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }
}
