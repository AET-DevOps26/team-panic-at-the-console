package com.panicattheconsole.gateway.proxy;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Forwards gateway API calls to cluster-internal services and preserves status codes.
 */
final class DownstreamProxy {

    private DownstreamProxy() {}

    static <T> ResponseEntity<T> get(RestClient client, String path, Class<T> bodyType, Object... uriVariables) {
        ResponseEntity<T> downstream = client.get()
                .uri(path, uriVariables)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }

    static <T> ResponseEntity<T> get(
            RestClient client, String path, ParameterizedTypeReference<T> bodyType, Object... uriVariables) {
        ResponseEntity<T> downstream = client.get()
                .uri(path, uriVariables)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }

    static <T> ResponseEntity<T> post(
            RestClient client, String path, Class<T> bodyType, Object... uriVariables) {
        return post(client, path, null, bodyType, uriVariables);
    }

    static <T> ResponseEntity<T> post(
            RestClient client, String path, Object requestBody, Class<T> bodyType, Object... uriVariables) {
        var request = client.post().uri(path, uriVariables);
        if (requestBody != null) {
            request = request.body(requestBody);
        }
        ResponseEntity<T> downstream = request.retrieve()
                .onStatus(HttpStatusCode::isError, (request1, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }

    static <T> ResponseEntity<T> patch(
            RestClient client, String path, Object requestBody, Class<T> bodyType, Object... uriVariables) {
        ResponseEntity<T> downstream = client.patch()
                .uri(path, uriVariables)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }

    // Cookie-aware variants: forward inbound Cookie header and copy Set-Cookie from response.

    static <T> ResponseEntity<T> postForwardingCookies(
            RestClient client, String path, Object requestBody, Class<T> bodyType, String cookieHeader) {
        ResponseEntity<T> downstream = client.post()
                .uri(path)
                .headers(h -> { if (cookieHeader != null) h.set(HttpHeaders.COOKIE, cookieHeader); })
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {})
                .toEntity(bodyType);
        return copySetCookie(downstream);
    }

    static <T> ResponseEntity<T> postForwardingCookies(
            RestClient client, String path, Class<T> bodyType, String cookieHeader) {
        ResponseEntity<T> downstream = client.post()
                .uri(path)
                .headers(h -> { if (cookieHeader != null) h.set(HttpHeaders.COOKIE, cookieHeader); })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {})
                .toEntity(bodyType);
        return copySetCookie(downstream);
    }

    static <T> ResponseEntity<T> getForwardingCookies(
            RestClient client, String path, Class<T> bodyType, String cookieHeader, Object... uriVariables) {
        ResponseEntity<T> downstream = client.get()
                .uri(path, uriVariables)
                .headers(h -> { if (cookieHeader != null) h.set(HttpHeaders.COOKIE, cookieHeader); })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {})
                .toEntity(bodyType);
        return copySetCookie(downstream);
    }

    private static <T> ResponseEntity<T> copySetCookie(ResponseEntity<T> downstream) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(downstream.getStatusCode());
        List<String> setCookies = downstream.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            for (String cookie : setCookies) {
                builder.header(HttpHeaders.SET_COOKIE, cookie);
            }
        }
        return builder.body(downstream.getBody());
    }
}
