package com.jypLord.config.r2dbc;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

@Configuration
public class R2dbcConfig{

    @Bean
    protected List<Object> getCustomConverters() {
        return List.of(
            new TradeStatusWritingConverter(),
            new TradeStatusReadingConverter()
        );
    }


}
