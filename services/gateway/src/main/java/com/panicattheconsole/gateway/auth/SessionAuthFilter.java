package com.panicattheconsole.gateway.auth;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import com.panicattheconsole.gateway.auth.SessionTokenValidator.SessionIdentity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enforces the session cookie on every API route except auth and health
 * (ADR 0007). On success the identity is stored as request attributes, which
 * {@link IdentityHeaderRelay} turns into X-User-Id / X-User-Role headers on
 * downstream calls. Unauthenticated requests get a 401 without ever reaching
 * a downstream service.
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_COOKIE = "session";
    public static final String USER_ID_ATTRIBUTE = SessionAuthFilter.class.getName() + ".userId";
    public static final String USER_ROLE_ATTRIBUTE = SessionAuthFilter.class.getName() + ".userRole";

    /** Paths (relative to the /api/v1 context) reachable without a session. */
    private static final Set<String> PUBLIC_PATHS =
            Set.of("/auth/register", "/auth/login", "/auth/logout", "/health");

    private final SessionTokenValidator validator;

    SessionAuthFilter(SessionTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie cookie = WebUtils.getCookie(request, SESSION_COOKIE);
        Optional<SessionIdentity> identity =
                cookie == null ? Optional.empty() : validator.validate(cookie.getValue());
        if (identity.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Not authenticated\"}");
            return;
        }

        request.setAttribute(USER_ID_ATTRIBUTE, identity.get().userId());
        request.setAttribute(USER_ROLE_ATTRIBUTE, identity.get().role());
        filterChain.doFilter(request, response);
    }

    private static boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PUBLIC_PATHS.contains(path);
    }
}
