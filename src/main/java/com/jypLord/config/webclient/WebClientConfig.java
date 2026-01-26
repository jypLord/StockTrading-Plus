package com.jypLord.config.webclient;

import com.jypLord.api.dto.request.buy.response.BuyResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Configuration
public class WebClientConfig {

    @Bean
    public ClientHttpConnector clientHttpConnector() {


        HttpClient httpClient = HttpClient.create()
            .compress(true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(1))
                    .addHandlerLast(new WriteTimeoutHandler(1))
            );

        return new ReactorClientHttpConnector(httpClient);
    }

    private ExchangeFilterFunction retryFilter(){
        return (request, next) ->
            next.exchange(request)
                .flatMap(response -> {
                    if(response.statusCode().is5xxServerError()){
                        return Mono.error(new RuntimeException("증권사 서버 에러"));
                    }

                    return Mono.just(response);
                })
                .retryWhen(
                    Retry.backoff(3, Duration.ofMillis(20))
                        .maxBackoff(Duration.ofMillis(100))
                        .jitter(0.2)
                );

    }
}

