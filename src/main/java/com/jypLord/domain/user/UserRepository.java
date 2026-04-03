package com.jypLord.domain.user;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT oauth_token FROM users WHERE id = :userId")
    Mono<String> findStockOAuthTokenById(@Param("userId") Long id);

    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByEmail(String email);
}
