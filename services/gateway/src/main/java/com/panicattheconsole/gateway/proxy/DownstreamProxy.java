package com.panicattheconsole.gateway.proxy;

import org.springframework.core.ParameterizedTypeReference;
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
}
