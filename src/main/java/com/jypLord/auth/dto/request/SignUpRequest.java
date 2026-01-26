package com.jypLord.auth.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class SignUpRequest {
    @Email
    private String email;

    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,20}$",
        message = "비밀번호는 소문자, 숫자, 특수문자를 포함하여 8~20자리여야 합니다"
    )
    private String password;

    private LocalDate birthday;

    private String name;
}
