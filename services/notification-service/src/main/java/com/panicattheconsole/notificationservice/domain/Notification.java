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

    /**
     * The user whose action produced this notification; {@code null} for
     * machine-triggered events. Actors never see notifications about their
     * own actions (read-side suppression for broadcasts).
     */
    @Column
    private UUID actorId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(
            UUID incidentId,
            NotificationType type,
            UUID recipientId,
            String message,
            Instant createdAt,
            UUID actorId) {
        this.id = UUID.randomUUID();
        this.incidentId = incidentId;
        this.type = type;
        this.recipientId = recipientId;
        this.message = message;
        this.createdAt = createdAt;
        this.actorId = actorId;
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

    public UUID getActorId() {
        return actorId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
