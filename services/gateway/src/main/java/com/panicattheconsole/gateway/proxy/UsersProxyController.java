package com.panicattheconsole.gateway.proxy;

import org.openapitools.api.UsersApi;
import org.openapitools.model.ChangePasswordRequest;
import org.openapitools.model.UpdateProfileRequest;
import org.openapitools.model.User;
import org.openapitools.model.UserListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@RestController
class UsersProxyController implements UsersApi {

    private final RestClient userServiceClient;
    private final HttpServletRequest httpRequest;

    UsersProxyController(
            @Qualifier("userServiceClient") RestClient userServiceClient,
            HttpServletRequest httpRequest) {
        this.userServiceClient = userServiceClient;
        this.httpRequest = httpRequest;
    }

    @Override
    public ResponseEntity<User> getCurrentUser() {
        return DownstreamProxy.getForwardingCookies(
                userServiceClient, "/users/me", User.class, cookie());
    }

    @Override
    public ResponseEntity<User> updateCurrentUser(UpdateProfileRequest updateProfileRequest) {
        return DownstreamProxy.patchForwardingCookies(
                userServiceClient, "/users/me", updateProfileRequest, User.class, cookie());
    }

    @Override
    public ResponseEntity<Void> changePassword(ChangePasswordRequest changePasswordRequest) {
        return DownstreamProxy.postForwardingCookies(
                userServiceClient, "/users/me/password", changePasswordRequest, Void.class, cookie());
    }

    @Override
    public ResponseEntity<UserListResponse> listUsers(Integer limit, Integer offset) {
        return DownstreamProxy.getForwardingCookies(
                userServiceClient, listUsersPath(limit, offset), UserListResponse.class, cookie());
    }

    private static String listUsersPath(Integer limit, Integer offset) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/users");
        if (limit != null) builder.queryParam("limit", limit);
        if (offset != null) builder.queryParam("offset", offset);
        return builder.build().toUriString();
    }

    private String cookie() {
        return httpRequest.getHeader(HttpHeaders.COOKIE);
    }
}
