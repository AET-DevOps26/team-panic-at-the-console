package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.openapitools.api.GenaiApi;
import org.openapitools.model.GenaiHealthResponse;
import org.openapitools.model.RegenAccepted;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Proxies OpenAPI genai routes: regen triggers to incident-service, Ollama health to genai-service.
 */
@RestController
class GenaiProxyController implements GenaiApi {

    private final RestClient incidentServiceClient;
    private final RestClient genaiServiceClient;

    GenaiProxyController(
            @Qualifier("incidentServiceClient") RestClient incidentServiceClient,
            @Qualifier("genaiServiceClient") RestClient genaiServiceClient) {
        this.incidentServiceClient = incidentServiceClient;
        this.genaiServiceClient = genaiServiceClient;
    }

    @Override
    public ResponseEntity<GenaiHealthResponse> genaiHealth() {
        return DownstreamProxy.get(
                genaiServiceClient, "/api/v1/genai/ollama/health", GenaiHealthResponse.class);
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSummary(UUID incidentId) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents/{incidentId}/genai/summary",
                RegenAccepted.class,
                incidentId);
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSeverity(UUID incidentId) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents/{incidentId}/genai/severity",
                RegenAccepted.class,
                incidentId);
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSolutions(UUID incidentId) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents/{incidentId}/genai/solutions",
                RegenAccepted.class,
                incidentId);
    }

    @Override
    public ResponseEntity<RegenAccepted> regeneratePostmortem(UUID incidentId) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents/{incidentId}/genai/postmortem",
                RegenAccepted.class,
                incidentId);
    }
}
