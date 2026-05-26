package com.panicattheconsole.gateway.proxy;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Forwards gateway API calls to cluster-internal services and preserves status codes.
 */
final class DownstreamProxy {

    private DownstreamProxy() {}

    static <T> ResponseEntity<T> post(RestClient client, String path, Class<T> bodyType, Object... uriVariables) {
        ResponseEntity<T> downstream = client.post()
                .uri(path, uriVariables)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }

    static <T> ResponseEntity<T> get(RestClient client, String path, Class<T> bodyType, Object... uriVariables) {
        ResponseEntity<T> downstream = client.get()
                .uri(path, uriVariables)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {})
                .toEntity(bodyType);
        return ResponseEntity.status(downstream.getStatusCode()).body(downstream.getBody());
    }
}
