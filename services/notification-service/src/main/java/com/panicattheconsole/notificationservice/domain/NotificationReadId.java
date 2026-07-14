package com.panicattheconsole.notificationservice.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class NotificationReadId implements Serializable {

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "user_id")
    private UUID userId;

    protected NotificationReadId() {
    }

    public NotificationReadId(UUID notificationId, UUID userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NotificationReadId other)) {
            return false;
        }
        return Objects.equals(notificationId, other.notificationId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, userId);
    }
}
