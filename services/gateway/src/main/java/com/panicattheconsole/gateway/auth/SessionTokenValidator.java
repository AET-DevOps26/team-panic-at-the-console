package com.panicattheconsole.gateway.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Validates session JWTs issued by user-service (ADR 0007). Shares the HMAC
 * secret with user-service via {@code auth.jwt.secret} / {@code AUTH_JWT_SECRET}.
 */
@Component
public class SessionTokenValidator {

    /** Identity claims carried by a valid session token. */
    public record SessionIdentity(String userId, String role) {}

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;

    SessionTokenValidator(@Value("${auth.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 characters (256-bit HMAC key)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Empty when the token is missing, malformed, tampered with, or expired. */
    public Optional<SessionIdentity> validate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String userId = claims.getSubject();
            String role = claims.get(ROLE_CLAIM, String.class);
            if (userId == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new SessionIdentity(userId, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
