package com.jypLord.domain.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {
    @Id
    private Long id;

    @Email
    private String email;

    @NotBlank
    private String password;

    private String name;

    @Column("birth_date")
    private LocalDate birthday;
    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("market_access_token")
    private String marketAccessToken;

    public User(String email, String password, String name, LocalDate birth_date, String oauthToken) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.birthday = birth_date;
        this.createdAt = LocalDateTime.now();
        this.marketAccessToken = oauthToken;
    }
    public User(String email, String password, String name, LocalDate birth_date) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.birthday = birth_date;
        this.createdAt = LocalDateTime.now();
        this.marketAccessToken = null;
    }


    public void setMarketAccessToken(String marketAccessToken) {
        this.marketAccessToken= marketAccessToken;
    }
}
