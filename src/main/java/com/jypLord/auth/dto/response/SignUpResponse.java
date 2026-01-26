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
public class SignUpResponse {
    private Long id;
    private String email;
    private String name;
    private LocalDate birthday;
    private LocalDateTime createdAt;

    public static SignUpResponse from(User user) {
        return SignUpResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .birthday(user.getBirthday())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
