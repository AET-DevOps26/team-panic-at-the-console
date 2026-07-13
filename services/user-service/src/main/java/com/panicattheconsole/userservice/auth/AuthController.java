package com.panicattheconsole.userservice.auth;

import org.openapitools.api.AuthApi;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.userservice.exception.InvalidInviteCodeException;
import com.panicattheconsole.userservice.users.UserAccount;
import com.panicattheconsole.userservice.users.UserAccountService;
import com.panicattheconsole.userservice.users.UserMapper;

/**
 * Auth endpoints ({@code /auth/register}, {@code /auth/login}, {@code /auth/logout}).
 * Login sets the httpOnly {@code session} JWT cookie per ADR 0007.
 */
@RestController
class AuthController implements AuthApi {

    private final UserAccountService users;
    private final SessionTokenService tokens;

    /** Blank = open registration; non-blank = required for every registration. */
    private final String inviteCode;

    AuthController(
            UserAccountService users,
            SessionTokenService tokens,
            @Value("${auth.invite-code:}") String inviteCode) {
        this.users = users;
        this.tokens = tokens;
        this.inviteCode = inviteCode;
    }

    @Override
    public ResponseEntity<User> registerUser(RegisterRequest registerRequest) {
        // Checked before the email so a wrong code cannot probe which
        // addresses already have accounts.
        if (!inviteCode.isBlank() && !inviteCode.equals(registerRequest.getInviteCode())) {
            throw new InvalidInviteCodeException();
        }
        UserAccount account = users.register(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getDisplayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toApi(account));
    }

    @Override
    public ResponseEntity<User> loginUser(LoginRequest loginRequest) {
        UserAccount account = users.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        String token = tokens.issue(account);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionCookies.session(token, tokens.getTtl()).toString())
                .body(UserMapper.toApi(account));
    }

    @Override
    public ResponseEntity<Void> logoutUser() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, SessionCookies.expired().toString())
                .build();
    }
}
