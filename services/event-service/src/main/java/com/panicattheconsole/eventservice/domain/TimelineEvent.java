package com.panicattheconsole.eventservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "timeline_events")
public class TimelineEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant eventTimestamp;

    protected TimelineEvent() {
    }

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;

    public TimelineEvent(
            UUID id,
            UUID incidentId,
            String eventType,
            Instant eventTimestamp,
            JsonNode payload) {
        this.id = id;
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
