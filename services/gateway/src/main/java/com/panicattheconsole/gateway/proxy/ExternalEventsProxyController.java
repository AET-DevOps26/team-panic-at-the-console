package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.openapitools.api.ExternalEventsApi;
import org.openapitools.model.ExternalEventDetail;
import org.openapitools.model.ExternalEventListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies the external-events audit routes to webhook-service. Session
 * authentication happens in the SessionAuthFilter like every other API route;
 * the webhook ingest endpoint itself is NOT proxied here (it is exposed
 * directly at /webhooks by the ingress / compose edge).
 */
@RestController
class ExternalEventsProxyController implements ExternalEventsApi {

    private final RestClient webhookServiceClient;

    ExternalEventsProxyController(@Qualifier("webhookServiceClient") RestClient webhookServiceClient) {
        this.webhookServiceClient = webhookServiceClient;
    }

    @Override
    public ResponseEntity<ExternalEventListResponse> listExternalEvents(
            String source, String eventType, Integer page, Integer size) {
        return DownstreamProxy.get(
                webhookServiceClient,
                listExternalEventsPath(source, eventType, page, size),
                ExternalEventListResponse.class);
    }

    @Override
    public ResponseEntity<ExternalEventDetail> getExternalEvent(UUID externalEventId) {
        return DownstreamProxy.get(
                webhookServiceClient,
                "/external-events/{externalEventId}",
                ExternalEventDetail.class,
                externalEventId);
    }

    private static String listExternalEventsPath(String source, String eventType, Integer page, Integer size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/external-events");
        if (source != null) builder.queryParam("source", source);
        if (eventType != null) builder.queryParam("eventType", eventType);
        if (page != null) builder.queryParam("page", page);
        if (size != null) builder.queryParam("size", size);
        return builder.build().toUriString();
    }
}
