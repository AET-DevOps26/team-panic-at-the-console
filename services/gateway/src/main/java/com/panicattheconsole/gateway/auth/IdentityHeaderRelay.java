package com.panicattheconsole.gateway.auth;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Adds X-User-Id / X-User-Role to downstream service calls from the identity
 * that {@link SessionAuthFilter} validated (ADR 0007). Reads server-set request
 * attributes, never inbound headers, so clients cannot spoof an identity.
 */
@Component
public class IdentityHeaderRelay implements ClientHttpRequestInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object userId = attributes.getAttribute(
                    SessionAuthFilter.USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            Object role = attributes.getAttribute(
                    SessionAuthFilter.USER_ROLE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (userId != null) {
                request.getHeaders().set(USER_ID_HEADER, userId.toString());
            }
            if (role != null) {
                request.getHeaders().set(USER_ROLE_HEADER, role.toString());
            }
        }
        return execution.execute(request, body);
    }
}
