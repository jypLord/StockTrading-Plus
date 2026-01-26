package com.jypLord.auth;

import com.jypLord.auth.dto.request.LoginRequest;
import com.jypLord.auth.dto.response.LoginResult;
import com.jypLord.auth.jwt.JwtProvider;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.auth.dto.request.SignUpRequest;
import com.jypLord.auth.dto.response.SignUpResponse;
import com.jypLord.exception.user.DuplicateSignUpException;
import com.jypLord.exception.user.FailedSaveRefreshTokenException;
import com.jypLord.exception.user.InvalidPasswordException;
import com.jypLord.exception.user.NoUserLoginException;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private final ReactiveRedisTemplate<String, String> redis;
    private final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);


    public Mono<SignUpResponse> signUp(SignUpRequest dto) {

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        return userRepository.existsByEmail(dto.getEmail())
            .flatMap(exist->{
                if(exist) {
                    return Mono.error(new DuplicateSignUpException("존재하는 이메일"));
                }

                return userRepository.save(new User(dto.getEmail(), encodedPassword , dto.getName(), dto.getBirthday()));
            })
            .map(SignUpResponse::from);
    }

    public Mono<LoginResult> login(LoginRequest dto) {
        return userRepository.findByEmail(dto.getEmail())
            .switchIfEmpty(Mono.error(new NoUserLoginException("사용자를 찾을 수 없음")))

            .filter(user -> passwordEncoder.matches(dto.getLawPassword(), user.getPassword()))

            .switchIfEmpty(Mono.error(new InvalidPasswordException("비밀번호 불일치")))

            // 토큰 발급
            .flatMap(user -> {

                String accessToken = jwtProvider.generateAccessToken(user.getEmail());

                return redis.opsForValue().get("refresh:" + user.getEmail())
                    // Refresh 토큰 없으면 새로 발급
                    .switchIfEmpty(Mono.defer(()-> Mono.just(jwtProvider.generateRefreshToken(user.getEmail()))))
                    .map(refreshToken -> LoginResult.of(user, accessToken, refreshToken));

            })
            .flatMap(login->  saveRefreshTokenToRedis(login.getRefreshToken(), login.getEmail())
                .thenReturn(login));
    }

    public Mono<Void> saveRefreshTokenToRedis(String refreshToken, String email) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new IllegalArgumentException("RefreshToken 값이 비어있음"));
        }

        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("Email 값이 비어있음"));
        }

        return redis.opsForValue()
            .set("refresh:" + email, refreshToken, REFRESH_TOKEN_TTL)
            .flatMap(success -> {
                if (Boolean.FALSE.equals(success)) {
                    return Mono.error(new FailedSaveRefreshTokenException("Redis 저장 실패"));
                }
                return Mono.just(true);
            })
            .retryWhen(
                Retry.backoff(3, Duration.ofMillis(300))
                    .maxBackoff(Duration.ofSeconds(2))
                    .jitter(0.3)
                    .filter(throwable ->
                        throwable instanceof io.lettuce.core.RedisCommandTimeoutException ||
                            throwable instanceof java.net.ConnectException
                    )
            )
            .timeout(Duration.ofSeconds(2))
            .doOnNext(ok -> log.info("RefreshToken 저장 성공 email={} at {}", email, LocalDateTime.now()))
            .doOnError(e -> {
                if (e instanceof io.lettuce.core.RedisCommandTimeoutException) {
                    log.error("Redis Timeout 발생 email={} reason={}", email, e.toString());
                } else if (e instanceof java.net.ConnectException) {
                    log.error("Redis 서버 연결 불가 email={} reason={}", email, e.toString());
                } else if (e instanceof org.springframework.data.redis.RedisSystemException) {
                    log.error("Redis 직렬화 문제 email={} reason={}", email, e.toString());
                } else {
                    log.error("RefreshToken Redis 저장 실패 email={} reason={}", email, e.toString());
                }
            })
            .then();
    }
}
