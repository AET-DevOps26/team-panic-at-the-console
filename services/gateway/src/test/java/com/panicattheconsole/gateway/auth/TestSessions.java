package com.panicattheconsole.gateway.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

/** Builds session cookies signed with the test secret from application.properties. */
public final class TestSessions {

    /** Must match auth.jwt.secret in src/test/resources/application.properties. */
    public static final String SECRET = "test-secret-that-is-at-least-32-characters";

    public static final String USER_ID = "018e2c5f-1234-7abc-8def-0000000000aa";
    public static final String ROLE = "MEMBER";

    private TestSessions() {}

    public static Cookie sessionCookie() {
        return new Cookie("session", token());
    }

    public static String token() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(USER_ID)
                .claim("role", ROLE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static String expiredToken() {
        Instant past = Instant.now().minusSeconds(7200);
        return Jwts.builder()
                .subject(USER_ID)
                .claim("role", ROLE)
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
