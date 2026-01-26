package com.jypLord.auth.dto.response;

import com.jypLord.auth.dto.request.LoginRequest;
import com.jypLord.domain.user.User;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private Long id;
    private String email;
    private String name;
    private String accessToken;
    private LocalDateTime loginAt;

    public static LoginResponse of(LoginResult loginResult) {
        return LoginResponse.builder()
            .id(loginResult.getId())
            .email(loginResult.getEmail())
            .name(loginResult.getName())
            .accessToken(loginResult.getAccessToken())
            .loginAt(loginResult.getLoginAt())
            .build();
    }
}