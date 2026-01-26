package com.jypLord.config.r2dbc;

import com.jypLord.domain.trade.TradeStatus;
import org.springframework.core.convert.converter.Converter;

public class TradeStatusWritingConverter implements Converter<TradeStatus, String> {
    @Override
    public String convert(TradeStatus source) {
        return source.name();  // Enum → String
    }
}
