package com.panicattheconsole.notificationservice.api;

import java.time.Instant;
import java.util.UUID;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.domain.NotificationType;

/**
 * REST representation of a stored notification. {@code recipientId} is null for
 * broadcast notifications visible to everyone.
 */
public record NotificationResponse(
        UUID id,
        UUID incidentId,
        NotificationType type,
        UUID recipientId,
        String message,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getIncidentId(),
                n.getType(),
                n.getRecipientId(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt());
    }
}
