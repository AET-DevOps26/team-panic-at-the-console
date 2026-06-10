package com.panicattheconsole.eventservice.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "incident_events", indexes = {
        @Index(columnList = "incident_id, timestamp")
})
class IncidentEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1024)
    private String description;

    protected IncidentEventRecord() {
    }

    IncidentEventRecord(UUID incidentId, Instant timestamp, String type, String description) {
        this.incidentId = incidentId;
        this.timestamp = timestamp;
        this.type = type;
        this.description = description;
    }

    UUID getIncidentId() { return incidentId; }
    Instant getTimestamp() { return timestamp; }
    String getType() { return type; }
    String getDescription() { return description; }
}
