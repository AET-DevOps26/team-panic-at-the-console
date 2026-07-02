package com.panicattheconsole.notificationservice.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Target user for a personal notification (e.g. assignment).
     * {@code null} means a broadcast notification visible to everyone.
     */
    @Column
    private UUID recipientId;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(
            UUID incidentId,
            NotificationType type,
            UUID recipientId,
            String message,
            Instant createdAt) {
        this.id = UUID.randomUUID();
        this.incidentId = incidentId;
        this.type = type;
        this.recipientId = recipientId;
        this.message = message;
        this.read = false;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public NotificationType getType() {
        return type;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
