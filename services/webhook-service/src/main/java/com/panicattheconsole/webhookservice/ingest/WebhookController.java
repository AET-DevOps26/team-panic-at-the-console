package com.panicattheconsole.webhookservice.ingest;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;

import org.openapitools.model.ErrorResponse;
import org.openapitools.model.WebhookReceipt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.webhookservice.event.ExternalEvent;
import com.panicattheconsole.webhookservice.event.ExternalEventService;
import com.panicattheconsole.webhookservice.event.ExternalEventService.IngestResult;
import com.panicattheconsole.webhookservice.ingest.SignatureVerifier.Decision;
import com.panicattheconsole.webhookservice.publish.ExternalEventPublisher;
import com.panicattheconsole.webhookservice.source.WebhookSource;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Receives webhooks from external systems (e.g. GitHub Actions): verifies the
 * signature, persists the payload verbatim as an External Event, and publishes
 * {@code external.event.received} for the rule engine.
 *
 * <p>Serves the {@code POST /webhooks/{source}} operation from the OpenAPI
 * spec, but deliberately does NOT implement the generated {@code WebhooksApi}
 * interface: the generated signature hands over a parsed body, while HMAC
 * verification needs the exact raw request bytes (re-serialising loses
 * whitespace and key order). Response bodies use the generated models, so the
 * wire contract still matches the spec.
 */
@RestController
public class WebhookController {

    /** Matches the external_events.delivery_id column length. */
    private static final int MAX_DELIVERY_ID_LENGTH = 255;

    private final SignatureVerifier signatureVerifier;
    private final EventTypeNormalizer eventTypeNormalizer;
    private final ExternalEventService eventService;
    private final ExternalEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    WebhookController(SignatureVerifier signatureVerifier, EventTypeNormalizer eventTypeNormalizer,
            ExternalEventService eventService, ExternalEventPublisher publisher,
            ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.signatureVerifier = signatureVerifier;
        this.eventTypeNormalizer = eventTypeNormalizer;
        this.eventService = eventService;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/webhooks/{source}")
    ResponseEntity<?> receive(
            @PathVariable String source,
            @RequestHeader(value = SignatureVerifier.SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = EventTypeNormalizer.GITHUB_EVENT_HEADER, required = false) String githubEvent,
            @RequestHeader(value = EventTypeNormalizer.EVENT_TYPE_HEADER, required = false) String eventTypeHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String githubDelivery,
            @RequestHeader(value = "X-Delivery-Id", required = false) String genericDelivery,
            @RequestBody byte[] rawBody) {
        if (!WebhookSource.SLUG_PATTERN.matcher(source).matches()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("invalid source: expected a lowercase slug"));
        }

        Decision decision = signatureVerifier.check(source, signature, rawBody);
        if (decision instanceof Decision.Rejected(String reason)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(reason));
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("request body is not valid JSON"));
        }
        if (payload == null || !payload.isObject()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("payload must be a JSON object"));
        }

        String eventType = eventTypeNormalizer.normalize(githubEvent, eventTypeHeader, payload);
        IngestResult result = eventService.ingest(
                source, deliveryId(githubDelivery, genericDelivery), eventType,
                new String(rawBody, StandardCharsets.UTF_8));

        meterRegistry.counter("webhooks.received", "source_type", source).increment();
        if (!result.duplicate()) {
            publisher.publish(result.event());
        }

        ExternalEvent event = result.event();
        return ResponseEntity.accepted().body(new WebhookReceipt(
                event.getId(), event.getSource(), event.getEventType(),
                event.getReceivedAt().atOffset(ZoneOffset.UTC), result.duplicate()));
    }

    private static String deliveryId(String githubDelivery, String genericDelivery) {
        String value = githubDelivery != null && !githubDelivery.isBlank() ? githubDelivery : genericDelivery;
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= MAX_DELIVERY_ID_LENGTH ? trimmed : trimmed.substring(0, MAX_DELIVERY_ID_LENGTH);
    }
}
