package com.panicattheconsole.eventservice.nats;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.eventservice.event.IncidentEventService;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Subscribes to incident.> and appends an IncidentEvent for each message received.
 * Uses a queue group so multiple replicas share the load without duplicating events.
 */
@Component
public class NatsIncidentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NatsIncidentEventConsumer.class);
    private static final String QUEUE_GROUP = "event-service";

    private final Connection natsConnection;
    private final IncidentEventService eventService;
    private final ObjectMapper objectMapper;
    private Dispatcher dispatcher;

    public NatsIncidentEventConsumer(Connection natsConnection,
                                     IncidentEventService eventService,
                                     ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void subscribe() {
        dispatcher = natsConnection.createDispatcher(message -> {
            String subject = message.getSubject();
            String raw = new String(message.getData(), StandardCharsets.UTF_8);
            try {
                Map<String, Object> payload = objectMapper.readValue(
                        raw, new TypeReference<>() {});

                UUID incidentId = UUID.fromString((String) payload.get("incidentId"));
                Instant timestamp = Instant.parse((String) payload.get("timestamp"));
                String type = subjectToType(subject);
                String description = buildDescription(subject, payload);

                if (type == null) {
                    log.debug("Ignoring untracked subject [{}]", subject);
                    return;
                }

                eventService.append(incidentId, timestamp, type, description);
                log.debug("Appended event [subject={}, incidentId={}]", subject, incidentId);
            } catch (Exception e) {
                log.error("Failed to process NATS message [subject={}, payload={}]", subject, raw, e);
            }
        });

        // incident.> matches all multi-level subjects under incident.*
        dispatcher.subscribe("incident.>", QUEUE_GROUP);
        log.info("Subscribed to incident.> (queue group: {})", QUEUE_GROUP);
    }

    @PreDestroy
    void unsubscribe() {
        if (dispatcher != null) {
            dispatcher.unsubscribe("incident.>");
        }
    }

    private String subjectToType(String subject) {
        return switch (subject) {
            case "incident.created"            -> "incident_created";
            case "incident.updated"            -> "incident_updated";
            case "incident.severity.escalated" -> "severity_escalated";
            case "incident.resolved"           -> "incident_resolved";
            case "incident.comment.added"      -> "comment_added";
            case "incident.assigned"           -> "assigned";
            default                            -> null;
        };
    }

    private String buildDescription(String subject, Map<String, Object> payload) {
        return switch (subject) {
            case "incident.created"            -> "Incident created";
            case "incident.updated"            -> "Incident updated";
            case "incident.severity.escalated" -> "Severity escalated to " + payload.get("newSeverity");
            case "incident.resolved"           -> "Incident resolved";
            case "incident.comment.added"      -> "Comment added";
            case "incident.assigned"           -> "Assigned to user " + payload.get("userId");
            default                            -> subject;
        };
    }
}
