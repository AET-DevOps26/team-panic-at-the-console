package com.panicattheconsole.userservice.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

/**
 * Builds the httpOnly {@code session} cookie per ADR 0007. No {@code Secure}
 * flag: TLS terminates at the ingress and the Azure VM deploy serves plain
 * HTTP; SameSite=Strict plus httpOnly is the agreed baseline.
 */
public final class SessionCookies {

    public static final String SESSION_COOKIE = "session";

    private SessionCookies() {}

    public static ResponseCookie session(String token, Duration ttl) {
        return builder(token).maxAge(ttl).build();
    }

    public static ResponseCookie expired() {
        return builder("").maxAge(0).build();
    }

    private static ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(SESSION_COOKIE, value)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/");
    }
}
