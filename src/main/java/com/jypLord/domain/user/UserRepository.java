package com.jypLord.domain.user;


import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;


public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT oauth_token FROM User WHERE id := userId")
    Mono<String> findStockOAuthTokenById(@Param("userId") Long id);

    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByEmail(String email);

}
