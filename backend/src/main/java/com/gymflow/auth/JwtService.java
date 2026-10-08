package com.gymflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite y valida los JWT de sesión. Claims: sub (userId), tenantId, role, email.
 * El tenant de cada petición autenticada sale de aquí, nunca de una cabecera del cliente.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${gymflow.jwt.secret}") String secret,
            @Value("${gymflow.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(UUID userId, UUID tenantId, String role, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role)
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact();
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return new ParsedToken(
            UUID.fromString(claims.getSubject()),
            UUID.fromString(claims.get("tenantId", String.class)),
            claims.get("role", String.class),
            claims.get("email", String.class));
    }

    public record ParsedToken(UUID userId, UUID tenantId, String role, String email) {
    }
}
