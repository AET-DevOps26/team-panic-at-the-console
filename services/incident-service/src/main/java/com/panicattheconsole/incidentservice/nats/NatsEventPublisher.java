package com.panicattheconsole.incidentservice.nats;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;

/**
 * Publishes incident events to NATS JetStream.
 * Events are thin by default: {incidentId, timestamp}.
 */
@Service
public class NatsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NatsEventPublisher.class);

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public NatsEventPublisher(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish incident.created event.
     */
    public void publishIncidentCreated(UUID incidentId) {
        publishEvent("incident.created", createBaseEvent(incidentId));
    }

    /**
     * Publish incident.updated event.
     */
    public void publishIncidentUpdated(UUID incidentId) {
        publishEvent("incident.updated", createBaseEvent(incidentId));
    }

    /**
     * Publish incident.severity.escalated event.
     */
    public void publishIncidentSeverityEscalated(UUID incidentId, String newSeverity) {
        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("newSeverity", newSeverity);
        publishEvent("incident.severity.escalated", event);
    }

    /**
     * Publish incident.resolved event.
     */
    public void publishIncidentResolved(UUID incidentId) {
        publishEvent("incident.resolved", createBaseEvent(incidentId));
    }

    /**
     * Publish incident.comment.added event.
     */
    public void publishIncidentCommentAdded(UUID incidentId, UUID commentId) {
        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("commentId", commentId.toString());
        publishEvent("incident.comment.added", event);
    }

    /**
     * Publish incident.assigned event.
     */
    public void publishIncidentAssigned(UUID incidentId, UUID userId) {
        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("userId", userId.toString());
        publishEvent("incident.assigned", event);
    }

    /**
     * Publish incident.regen.requested event.
     */
    public void publishIncidentRegenRequested(UUID incidentId) {
        publishEvent("incident.regen.requested", createBaseEvent(incidentId));
    }

    private Map<String, Object> createBaseEvent(UUID incidentId) {
        Map<String, Object> event = new HashMap<>();
        event.put("incidentId", incidentId.toString());
        event.put("timestamp", Instant.now().toString());
        return event;
    }

    private void publishEvent(String subject, Map<String, Object> event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing NATS event [subject={}, payload={}]", subject, payload);
            natsConnection.publish(subject, payload.getBytes());
        } catch (Exception e) {
            log.error("Failed to publish NATS event [subject={}]", subject, e);
            // Log but don't throw - allow response to proceed
            // In production, you might want to retry or dead-letter this
        }
    }
}
