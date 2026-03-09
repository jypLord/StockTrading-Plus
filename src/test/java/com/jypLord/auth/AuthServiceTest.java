package com.jypLord.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jypLord.auth.dto.request.LoginRequest;
import com.jypLord.auth.dto.request.SignUpRequest;
import com.jypLord.domain.user.User;
import com.jypLord.domain.user.UserRepository;
import com.jypLord.exception.user.DuplicateSignUpException;
import com.jypLord.exception.user.FailedSaveRefreshTokenException;
import com.jypLord.exception.user.InvalidPasswordException;
import com.jypLord.exception.user.NoUserLoginException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private com.jypLord.auth.jwt.JwtProvider jwtProvider;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private ReactiveRedisTemplate<String, String> redis;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void signUp_success() {
        SignUpRequest request = new SignUpRequest();
        ReflectionTestUtils.setField(request, "email", "user@test.com");
        ReflectionTestUtils.setField(request, "password", "raw-password");
        ReflectionTestUtils.setField(request, "name", "tester");
        ReflectionTestUtils.setField(request, "birthday", LocalDate.of(2000, 1, 1));

        User savedUser = new User(1L, "user@test.com", "encoded", "tester", LocalDate.of(2000, 1, 1), null, null);

        given(passwordEncoder.encode("raw-password")).willReturn("encoded");
        given(userRepository.existsByEmail("user@test.com")).willReturn(Mono.just(false));
        given(userRepository.save(any(User.class))).willReturn(Mono.just(savedUser));

        StepVerifier.create(authService.signUp(request))
            .assertNext(response -> {
                assertEquals(1L, response.getId());
                assertEquals("user@test.com", response.getEmail());
            })
            .verifyComplete();
    }

    @Test
    void signUp_duplicateEmail_throwsException() {
        SignUpRequest request = new SignUpRequest();
        ReflectionTestUtils.setField(request, "email", "user@test.com");
        ReflectionTestUtils.setField(request, "password", "raw-password");

        given(passwordEncoder.encode("raw-password")).willReturn("encoded");
        given(userRepository.existsByEmail("user@test.com")).willReturn(Mono.just(true));

        StepVerifier.create(authService.signUp(request))
            .expectError(DuplicateSignUpException.class)
            .verify();

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_successWithExistingRefreshToken() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "user@test.com");
        ReflectionTestUtils.setField(request, "lawPassword", "raw-password");

        User user = new User(1L, "user@test.com", "encoded", "tester", LocalDate.of(2000, 1, 1), null, "market-token");

        given(redis.opsForValue()).willReturn(valueOperations);
        given(userRepository.findByEmail("user@test.com")).willReturn(Mono.just(user));
        given(passwordEncoder.matches("raw-password", "encoded")).willReturn(true);
        given(jwtProvider.generateAccessToken("user@test.com")).willReturn("access-token");
        given(valueOperations.get("refresh:user@test.com")).willReturn(Mono.just("refresh-token"));
        given(valueOperations.set(eq("refresh:user@test.com"), eq("refresh-token"), any())).willReturn(Mono.just(true));

        StepVerifier.create(authService.login(request))
            .assertNext(result -> {
                assertEquals("access-token", result.getAccessToken());
                assertEquals("refresh-token", result.getRefreshToken());
            })
            .verifyComplete();
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "missing@test.com");
        ReflectionTestUtils.setField(request, "lawPassword", "raw-password");

        given(userRepository.findByEmail("missing@test.com")).willReturn(Mono.empty());

        StepVerifier.create(authService.login(request))
            .expectError(NoUserLoginException.class)
            .verify();
    }

    @Test
    void login_passwordMismatch_throwsException() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "user@test.com");
        ReflectionTestUtils.setField(request, "lawPassword", "wrong");

        User user = new User(1L, "user@test.com", "encoded", "tester", LocalDate.of(2000, 1, 1), null, "market-token");

        given(userRepository.findByEmail("user@test.com")).willReturn(Mono.just(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        StepVerifier.create(authService.login(request))
            .expectError(InvalidPasswordException.class)
            .verify();
    }

    @Test
    void saveRefreshTokenToRedis_blankToken_throwsIllegalArgument() {
        StepVerifier.create(authService.saveRefreshTokenToRedis(" ", "user@test.com"))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void saveRefreshTokenToRedis_redisReturnsFalse_throwsFailedSaveException() {
        given(redis.opsForValue()).willReturn(valueOperations);
        given(valueOperations.set(eq("refresh:user@test.com"), eq("token"), any())).willReturn(Mono.just(false));

        StepVerifier.create(authService.saveRefreshTokenToRedis("token", "user@test.com"))
            .expectError(FailedSaveRefreshTokenException.class)
            .verify();
    }
}
