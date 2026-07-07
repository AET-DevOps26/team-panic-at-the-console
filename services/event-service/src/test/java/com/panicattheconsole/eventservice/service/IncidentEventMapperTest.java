package com.panicattheconsole.eventservice.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.eventservice.api.IncidentEventDto;
import com.panicattheconsole.eventservice.domain.TimelineEvent;

class IncidentEventMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID incidentId = UUID.randomUUID();
    private final Instant timestamp = Instant.parse("2026-07-07T10:15:30Z");

    private TimelineEvent event(String type, Map<String, String> payload) {
        JsonNode node = objectMapper.valueToTree(payload);
        return new TimelineEvent(incidentId, type, timestamp, node);
    }

    @Test
    void mapsStatusChangeWithTransition() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(
                event("incident.status.changed", Map.of("oldStatus", "open", "newStatus", "investigating")));

        assertThat(dto).hasValueSatisfying(e -> {
            assertThat(e.type()).isEqualTo("status_changed");
            assertThat(e.description()).isEqualTo("status: open → investigating");
            assertThat(e.timestamp()).isEqualTo(timestamp);
        });
    }

    @Test
    void mapsSeverityEscalationWithBothLevels() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(
                event("incident.severity.escalated", Map.of("oldSeverity", "SEV2", "newSeverity", "SEV1")));

        assertThat(dto).hasValueSatisfying(e -> {
            assertThat(e.type()).isEqualTo("severity_changed");
            assertThat(e.description()).isEqualTo("severity: SEV2 → SEV1");
        });
    }

    @Test
    void mapsSeverityEscalationWithoutOldLevel_fromRowsStoredBeforeSchemaChange() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(
                event("incident.severity.escalated", Map.of("newSeverity", "SEV1")));

        assertThat(dto).hasValueSatisfying(e ->
                assertThat(e.description()).isEqualTo("severity changed to SEV1"));
    }

    @Test
    void mapsCreatedWithTitleAndSeverity() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(
                event("incident.created", Map.of("title", "Checkout 5xx spike", "severity", "SEV1")));

        assertThat(dto).hasValueSatisfying(e -> {
            assertThat(e.type()).isEqualTo("incident_created");
            assertThat(e.description()).isEqualTo("Incident created: Checkout 5xx spike (SEV1)");
        });
    }

    @Test
    void mapsCreatedWithoutEnrichment_fromRowsStoredBeforeSchemaChange() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(event("incident.created", Map.of()));

        assertThat(dto).hasValueSatisfying(e ->
                assertThat(e.description()).isEqualTo("Incident created"));
    }

    @Test
    void mapsCommentWithContent() {
        Optional<IncidentEventDto> dto = IncidentEventMapper.toApi(
                event("incident.comment.added", Map.of("content", "Rolled back v2.4.1")));

        assertThat(dto).hasValueSatisfying(e -> {
            assertThat(e.type()).isEqualTo("comment_added");
            assertThat(e.description()).isEqualTo("Rolled back v2.4.1");
        });
    }

    @Test
    void skipsGenericUpdatedAndRedundantResolvedEvents() {
        assertThat(IncidentEventMapper.toApi(event("incident.updated", Map.of()))).isEmpty();
        assertThat(IncidentEventMapper.toApi(event("incident.resolved", Map.of()))).isEmpty();
    }
}
