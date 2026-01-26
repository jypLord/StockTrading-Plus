package com.jypLord.auth.dto.response;

import com.jypLord.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LoginResult {
    private Long id;
    private String email;
    private String name;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime loginAt;

    public static LoginResult of(User user, String accessToken, String refreshToken) {
        return LoginResult.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .loginAt(LocalDateTime.now())
            .build();
    }
}
