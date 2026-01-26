package com.jypLord.auth.jwt;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 15;      // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 14; // 14일

    private final String secret;
    private final Key secretKey;
    private JwtParser parser;

    public JwtProvider(@Value("${jwt.secret.key}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT Secret Key가 비어있음");
        }

        this.secret = secret;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostConstruct
    void initParser() {
        this.parser = Jwts.parserBuilder().setSigningKey(secretKey).build();
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            parser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parser.parseClaimsJws(token).getBody().getSubject();
    }
}