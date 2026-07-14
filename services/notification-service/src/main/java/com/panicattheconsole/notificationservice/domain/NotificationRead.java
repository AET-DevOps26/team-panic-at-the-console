package com.panicattheconsole.notificationservice.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Per-user read mark: a row means the user has read the notification.
 * Broadcast notifications (null recipient) get one mark per reader, so read
 * state never leaks between users.
 */
@Entity
@Table(name = "notification_reads")
public class NotificationRead {

    @EmbeddedId
    private NotificationReadId id;

    @Column(nullable = false)
    private Instant readAt;

    protected NotificationRead() {
    }

    public NotificationRead(UUID notificationId, UUID userId, Instant readAt) {
        this.id = new NotificationReadId(notificationId, userId);
        this.readAt = readAt;
    }

    public NotificationReadId getId() {
        return id;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
