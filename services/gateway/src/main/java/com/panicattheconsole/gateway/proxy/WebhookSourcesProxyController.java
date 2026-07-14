package com.panicattheconsole.gateway.proxy;

import org.openapitools.api.WebhookSourcesApi;
import org.openapitools.model.CreateWebhookSourceRequest;
import org.openapitools.model.WebhookSourceListResponse;
import org.openapitools.model.WebhookSourceWithSecret;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Proxies self-service webhook source management to webhook-service. Session
 * authentication happens in the SessionAuthFilter like every other API route;
 * the secret in create/rotate responses passes through to the caller once and
 * is not logged or cached here.
 */
@RestController
class WebhookSourcesProxyController implements WebhookSourcesApi {

    private final RestClient webhookServiceClient;

    WebhookSourcesProxyController(@Qualifier("webhookServiceClient") RestClient webhookServiceClient) {
        this.webhookServiceClient = webhookServiceClient;
    }

    @Override
    public ResponseEntity<WebhookSourceListResponse> listWebhookSources() {
        return DownstreamProxy.get(webhookServiceClient, "/webhook-sources", WebhookSourceListResponse.class);
    }

    @Override
    public ResponseEntity<WebhookSourceWithSecret> createWebhookSource(CreateWebhookSourceRequest request) {
        return DownstreamProxy.post(
                webhookServiceClient, "/webhook-sources", request, WebhookSourceWithSecret.class);
    }

    @Override
    public ResponseEntity<WebhookSourceWithSecret> rotateWebhookSourceSecret(String source) {
        return DownstreamProxy.post(
                webhookServiceClient, "/webhook-sources/{source}/rotate-secret",
                WebhookSourceWithSecret.class, source);
    }

    @Override
    public ResponseEntity<Void> deleteWebhookSource(String source) {
        return DownstreamProxy.delete(webhookServiceClient, "/webhook-sources/{source}", source);
    }
}
