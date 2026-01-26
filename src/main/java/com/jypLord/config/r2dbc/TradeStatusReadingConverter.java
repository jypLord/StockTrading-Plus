package com.jypLord.config.r2dbc;

import com.jypLord.domain.trade.TradeStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class TradeStatusReadingConverter implements Converter<String, TradeStatus> {
    @Override
    public TradeStatus convert(String source) {
        return TradeStatus.valueOf(source);
    }
}