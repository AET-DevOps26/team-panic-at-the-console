package com.panicattheconsole.userservice.users;

import java.util.List;

import org.openapitools.api.UsersApi;
import org.openapitools.model.User;
import org.openapitools.model.UserListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import com.panicattheconsole.userservice.auth.SessionCookies;
import com.panicattheconsole.userservice.auth.SessionTokenService;
import com.panicattheconsole.userservice.auth.SessionTokenService.SessionUser;
import com.panicattheconsole.userservice.exception.NotAuthenticatedException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * User profile and directory endpoints ({@code /users/me}, {@code /users}).
 * Both require a valid {@code session} cookie; the gateway forwards it.
 */
@RestController
class UsersController implements UsersApi {

    private final UserAccountService users;
    private final SessionTokenService tokens;
    private final HttpServletRequest httpRequest;

    UsersController(UserAccountService users, SessionTokenService tokens, HttpServletRequest httpRequest) {
        this.users = users;
        this.tokens = tokens;
        this.httpRequest = httpRequest;
    }

    @Override
    public ResponseEntity<User> getCurrentUser() {
        SessionUser session = requireSession();
        UserAccount account = users.findById(session.userId())
                .orElseThrow(NotAuthenticatedException::new);
        return ResponseEntity.ok(UserMapper.toApi(account));
    }

    @Override
    public ResponseEntity<UserListResponse> listUsers(Integer limit, Integer offset) {
        requireSession();
        List<User> items = users.list(limit, offset).stream().map(UserMapper::toApi).toList();
        return ResponseEntity.ok(new UserListResponse(items, (int) users.count(), limit, offset));
    }

    private SessionUser requireSession() {
        Cookie cookie = WebUtils.getCookie(httpRequest, SessionCookies.SESSION_COOKIE);
        if (cookie == null) {
            throw new NotAuthenticatedException();
        }
        return tokens.parse(cookie.getValue()).orElseThrow(NotAuthenticatedException::new);
    }
}
