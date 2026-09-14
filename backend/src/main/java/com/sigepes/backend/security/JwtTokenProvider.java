package com.sigepes.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Emisión y verificación de los access token JWT (HS256). */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${sigepes.security.jwt-secret}") String secret,
            @Value("${sigepes.security.expiration-seconds:86400}") long expirationSeconds) {
        byte[] material = secret.getBytes(StandardCharsets.UTF_8);
        if (material.length < 32) {
            throw new IllegalStateException(
                    "sigepes.security.jwt-secret debe tener al menos 32 caracteres (256 bits) para HS256");
        }
        this.key = Keys.hmacShaKeyFor(material);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(String userId, String email, String fullName, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("name", fullName)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
