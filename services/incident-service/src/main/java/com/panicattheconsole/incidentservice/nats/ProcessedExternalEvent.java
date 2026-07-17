package com.panicattheconsole.incidentservice.nats;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "processed_external_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_external_events_event_id", columnNames = "external_event_id")
})
public class ProcessedExternalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_event_id", nullable = false, updatable = false, unique = true)
    private String externalEventId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedExternalEvent() {
    }

    public ProcessedExternalEvent(String externalEventId) {
        this.externalEventId = externalEventId;
        this.processedAt = Instant.now();
    }

    public String getExternalEventId() {
        return externalEventId;
    }
}
