package com.panicattheconsole.webhookservice.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.openapitools.api.ExternalEventsApi;
import org.openapitools.model.ExternalEventDetail;
import org.openapitools.model.ExternalEventListResponse;
import org.openapitools.model.ExternalEventSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Read access to the persisted External Events (the audit trail from
 * ADR 0008), serving the generated `external-events` API. Reached by users
 * through the gateway proxy (session-authenticated there); the raw payload is
 * only returned on the single-event endpoint.
 */
@RestController
class ExternalEventController implements ExternalEventsApi {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExternalEventService eventService;
    private final ObjectMapper objectMapper;

    ExternalEventController(ExternalEventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<ExternalEventListResponse> listExternalEvents(
            String source, String eventType, Integer page, Integer size) {
        // Bean Validation on the generated interface is not active here, so
        // clamp defensively instead of trusting the @Min/@Max annotations.
        int boundedPage = Math.max(page == null ? 0 : page, 0);
        int boundedSize = Math.clamp(size == null ? 50 : size, 1, MAX_PAGE_SIZE);
        Page<ExternalEvent> events = eventService.list(blankToNull(source), blankToNull(eventType),
                PageRequest.of(boundedPage, boundedSize, Sort.by(Sort.Direction.DESC, "receivedAt")));
        return ResponseEntity.ok(new ExternalEventListResponse()
                .items(events.getContent().stream().map(ExternalEventController::toSummary).toList())
                .total(Math.toIntExact(events.getTotalElements()))
                .page(events.getNumber())
                .size(events.getSize()));
    }

    @Override
    public ResponseEntity<ExternalEventDetail> getExternalEvent(UUID externalEventId) {
        return eventService.get(externalEventId)
                .map(event -> ResponseEntity.ok(toDetail(event)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ExternalEventSummary toSummary(ExternalEvent event) {
        return new ExternalEventSummary()
                .id(event.getId())
                .source(event.getSource())
                .eventType(event.getEventType())
                .deliveryId(event.getDeliveryId())
                .receivedAt(atUtc(event.getReceivedAt()))
                .publishedAt(atUtc(event.getPublishedAt()));
    }

    private ExternalEventDetail toDetail(ExternalEvent event) {
        return new ExternalEventDetail()
                .id(event.getId())
                .source(event.getSource())
                .eventType(event.getEventType())
                .deliveryId(event.getDeliveryId())
                .receivedAt(atUtc(event.getReceivedAt()))
                .publishedAt(atUtc(event.getPublishedAt()))
                .rawPayload(parse(event.getRawPayload()));
    }

    private static OffsetDateTime atUtc(java.time.Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Object parse(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException e) {
            // Stored payloads were validated on ingest; failing here means data corruption.
            throw new IllegalStateException("stored raw payload is not valid JSON", e);
        }
    }
}
