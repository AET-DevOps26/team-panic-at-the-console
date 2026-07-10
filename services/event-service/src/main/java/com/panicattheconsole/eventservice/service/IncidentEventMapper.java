package com.panicattheconsole.eventservice.service;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.panicattheconsole.eventservice.api.IncidentEventDto;
import com.panicattheconsole.eventservice.domain.TimelineEvent;

/**
 * Maps stored NATS envelopes to public timeline entries.
 *
 * Not every stored event becomes a timeline entry: incident.updated is too
 * generic to describe (description edits, AI-field writes, ...) and
 * incident.resolved is redundant with the status_changed entry for the same
 * transition. Payload fields are read defensively because rows written before
 * a schema gained a field lack it.
 */
final class IncidentEventMapper {

    private IncidentEventMapper() {
    }

    static Optional<IncidentEventDto> toApi(TimelineEvent event) {
        String description = switch (event.getEventType()) {
            case "incident.created" -> describeCreated(event.getPayload());
            case "incident.status.changed" -> describeStatusChange(event.getPayload());
            case "incident.severity.escalated" -> describeSeverityChange(event.getPayload());
            case "incident.comment.added" -> text(event.getPayload(), "content").orElse("Comment added");
            case "incident.assigned" -> "User assigned";
            default -> null;
        };
        if (description == null) {
            return Optional.empty();
        }
        return Optional.of(new IncidentEventDto(
                event.getEventTimestamp(),
                toApiType(event.getEventType()),
                description,
                newValue(event)));
    }

    private static String newValue(TimelineEvent event) {
        return switch (event.getEventType()) {
            case "incident.status.changed" -> text(event.getPayload(), "newStatus").orElse(null);
            case "incident.severity.escalated" -> text(event.getPayload(), "newSeverity").orElse(null);
            default -> null;
        };
    }

    private static String toApiType(String eventType) {
        return switch (eventType) {
            case "incident.created" -> "incident_created";
            case "incident.status.changed" -> "status_changed";
            case "incident.severity.escalated" -> "severity_changed";
            case "incident.comment.added" -> "comment_added";
            case "incident.assigned" -> "assigned";
            default -> eventType;
        };
    }

    private static String describeCreated(JsonNode payload) {
        Optional<String> title = text(payload, "title");
        Optional<String> severity = text(payload, "severity");
        if (title.isPresent() && severity.isPresent()) {
            return "Incident created: " + title.get() + " (" + severity.get() + ")";
        }
        return "Incident created";
    }

    private static String describeStatusChange(JsonNode payload) {
        Optional<String> oldStatus = text(payload, "oldStatus");
        Optional<String> newStatus = text(payload, "newStatus");
        if (oldStatus.isPresent() && newStatus.isPresent()) {
            return "status: " + oldStatus.get() + " → " + newStatus.get();
        }
        return "Status changed";
    }

    private static String describeSeverityChange(JsonNode payload) {
        Optional<String> oldSeverity = text(payload, "oldSeverity");
        Optional<String> newSeverity = text(payload, "newSeverity");
        if (oldSeverity.isPresent() && newSeverity.isPresent()) {
            return "severity: " + oldSeverity.get() + " → " + newSeverity.get();
        }
        return newSeverity.map(s -> "severity changed to " + s).orElse("Severity changed");
    }

    private static Optional<String> text(JsonNode payload, String field) {
        if (payload == null) {
            return Optional.empty();
        }
        JsonNode node = payload.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(node.asText());
    }
}
