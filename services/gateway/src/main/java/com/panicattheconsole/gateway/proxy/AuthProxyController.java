package com.panicattheconsole.gateway.proxy;

import org.openapitools.api.AuthApi;
import org.openapitools.model.LoginRequest;
import org.openapitools.model.RegisterRequest;
import org.openapitools.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
class AuthProxyController implements AuthApi {

    private final RestClient userServiceClient;
    private final HttpServletRequest httpRequest;

    AuthProxyController(
            @Qualifier("userServiceClient") RestClient userServiceClient,
            HttpServletRequest httpRequest) {
        this.userServiceClient = userServiceClient;
        this.httpRequest = httpRequest;
    }

    @Override
    public ResponseEntity<User> registerUser(RegisterRequest registerRequest) {
        return DownstreamProxy.postForwardingCookies(
                userServiceClient, "/auth/register", registerRequest, User.class, cookie());
    }

    @Override
    public ResponseEntity<User> loginUser(LoginRequest loginRequest) {
        return DownstreamProxy.postForwardingCookies(
                userServiceClient, "/auth/login", loginRequest, User.class, cookie());
    }

    @Override
    public ResponseEntity<Void> logoutUser() {
        return DownstreamProxy.postForwardingCookies(
                userServiceClient, "/auth/logout", Void.class, cookie());
    }

    private String cookie() {
        return httpRequest.getHeader(HttpHeaders.COOKIE);
    }
}
