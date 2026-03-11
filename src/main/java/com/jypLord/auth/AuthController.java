package com.jypLord.auth;


import com.jypLord.auth.dto.request.LoginRequest;
import com.jypLord.auth.dto.request.SignUpRequest;
import com.jypLord.auth.dto.response.LoginResponse;
import com.jypLord.auth.dto.response.SignUpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Controller
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signUp")
    public Mono<ResponseEntity<SignUpResponse>> signUp(@RequestBody SignUpRequest dto) {
        return authService.signUp(dto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest dto,
        ServerHttpResponse response) {
        return authService.login(dto)
            .map(result -> {

                ResponseCookie cookie = ResponseCookie.from("refreshToken",
                        result.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .maxAge(Duration.ofDays(14))
                    .build();

                response.addCookie(cookie);


                return ResponseEntity.ok(LoginResponse.of(result));
            });
    }
}
