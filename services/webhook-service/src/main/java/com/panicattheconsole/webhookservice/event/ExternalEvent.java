package com.panicattheconsole.webhookservice.event;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A raw payload received from an external system (see CONTEXT.md), persisted
 * verbatim for auditability (ADR 0008). Immutable except for the NATS publish
 * bookkeeping ({@code publishedAt}/{@code publishAttempts}).
 */
@Entity
@Table(name = "external_events")
public class ExternalEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    /** Sender-supplied delivery id (e.g. X-GitHub-Delivery), used for dedup. */
    @Column(name = "delivery_id", length = 255)
    private String deliveryId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false)
    private String rawPayload;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    protected ExternalEvent() {
        // for JPA
    }

    public ExternalEvent(UUID id, String source, String eventType, String deliveryId,
            Instant receivedAt, String rawPayload) {
        this.id = id;
        this.source = source;
        this.eventType = eventType;
        this.deliveryId = deliveryId;
        this.receivedAt = receivedAt;
        this.rawPayload = rawPayload;
    }

    public void markPublished(Instant at) {
        this.publishedAt = at;
    }

    public void recordPublishFailure() {
        this.publishAttempts++;
    }

    public UUID getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getPublishAttempts() {
        return publishAttempts;
    }
}
