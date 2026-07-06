package com.panicattheconsole.notificationservice.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * A normalized incident event consumed from NATS. Carries the common envelope
 * fields ({@code incidentId}, {@code subject}, {@code timestamp}) plus the optional
 * per-subject identifiers that some events include (see api/specs/nats/*.schema.json).
 */
public record IncidentEvent(
        UUID incidentId,
        String subject,
        Instant timestamp,
        UUID assignedUserId,
        String newSeverity,
        UUID commentId) {
}
