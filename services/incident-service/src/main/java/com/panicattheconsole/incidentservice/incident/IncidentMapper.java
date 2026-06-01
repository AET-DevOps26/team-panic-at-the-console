package com.panicattheconsole.incidentservice.incident;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    static List<IncidentEvent> emptyEvents() {
        return List.of();
    }
}
