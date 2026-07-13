package com.panicattheconsole.userservice.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.openapitools.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.panicattheconsole.userservice.users.UserAccount;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates the signed session JWTs stored in the httpOnly
 * {@code session} cookie (ADR 0007). The HMAC secret is shared with the
 * gateway, which validates the same tokens.
 */
@Service
public class SessionTokenService {

    /** Authenticated identity extracted from a valid session token. */
    public record SessionUser(UUID userId, UserRole role) {}

    static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration ttl;

    public SessionTokenService(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.session-ttl}") Duration ttl) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 characters (256-bit HMAC key)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public Duration getTtl() {
        return ttl;
    }

    public String issue(UserAccount account) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(account.getId().toString())
                .claim(ROLE_CLAIM, account.getRole().getValue())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Empty when the token is missing, malformed, tampered with, or expired. */
    public Optional<SessionUser> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new SessionUser(
                    UUID.fromString(claims.getSubject()),
                    UserRole.fromValue(claims.get(ROLE_CLAIM, String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
