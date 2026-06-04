package com.panicattheconsole.incidentservice.incident;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.model.IncidentEvent;
import org.openapitools.model.IncidentStatus;
import org.openapitools.model.Severity;

final class IncidentMapper {

    private IncidentMapper() {
    }

    static org.openapitools.model.Incident toApi(Incident incident) {
        org.openapitools.model.Incident api = new org.openapitools.model.Incident(
                incident.getId(),
                incident.getTitle(),
                toApiStatus(incident.getStatus()),
                toApiSeverity(incident.getSeverity()),
                OffsetDateTime.ofInstant(incident.getCreatedAt(), ZoneOffset.UTC));

        if (incident.getSummary() != null) {
            api.setDescription(JsonNullable.of(incident.getSummary()));
        }
        if (incident.getResolvedAt() != null) {
            api.setResolvedAt(JsonNullable.of(
                    OffsetDateTime.ofInstant(incident.getResolvedAt(), ZoneOffset.UTC)));
        }
        return api;
    }

    static IncidentStatus toApiStatus(com.panicattheconsole.incidentservice.incident.IncidentStatus status) {
        return IncidentStatus.fromValue(status.name().toLowerCase());
    }

    static Severity toApiSeverity(com.panicattheconsole.incidentservice.incident.Severity severity) {
        return Severity.fromValue(severity.name());
    }

    static List<IncidentEvent> toApiEvents(Incident incident, List<Comment> comments) {
        List<IncidentEvent> events = new ArrayList<>();

        String title = incident.getTitle() != null ? incident.getTitle() : "(no title)";
        events.add(new IncidentEvent(
                OffsetDateTime.ofInstant(incident.getCreatedAt(), ZoneOffset.UTC),
                "incident_created",
                "Incident created: " + title + " (" + incident.getSeverity() + ")"));

        for (Comment comment : comments) {
            events.add(new IncidentEvent(
                    OffsetDateTime.ofInstant(comment.getCreatedAt(), ZoneOffset.UTC),
                    "comment_added",
                    comment.getContent()));
        }

        if (incident.getResolvedAt() != null) {
            events.add(new IncidentEvent(
                    OffsetDateTime.ofInstant(incident.getResolvedAt(), ZoneOffset.UTC),
                    "incident_resolved",
                    "status: " + incident.getStatus().name().toLowerCase()));
        }

        events.sort(Comparator.comparing(IncidentEvent::getTimestamp));
        return List.copyOf(events);
    }
}
