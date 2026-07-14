package com.panicattheconsole.notificationservice.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A normalized incident event consumed from NATS. Carries the common envelope
 * fields ({@code incidentId}, {@code subject}, {@code timestamp}) plus the optional
 * per-subject fields that some events include (see api/specs/nats/*.schema.json):
 * {@code assignedUserId} is the target of incident.assigned, {@code assignedUserIds}
 * is the incident's assignee set carried by escalation/status/comment events, and
 * {@code actorId} is the user whose action produced the event (null for
 * machine-triggered events).
 */
public record IncidentEvent(
        UUID incidentId,
        String subject,
        Instant timestamp,
        UUID assignedUserId,
        String newSeverity,
        UUID commentId,
        String title,
        String severity,
        String content,
        String newStatus,
        List<UUID> assignedUserIds,
        UUID actorId) {

    /** Named construction; the positional canonical constructor is unreadable at this arity. */
    public static class Builder {
        private final String subject;
        private UUID incidentId;
        private Instant timestamp;
        private UUID assignedUserId;
        private String newSeverity;
        private UUID commentId;
        private String title;
        private String severity;
        private String content;
        private String newStatus;
        private List<UUID> assignedUserIds;
        private UUID actorId;

        public Builder(String subject) {
            this.subject = subject;
        }

        public Builder incidentId(UUID incidentId) {
            this.incidentId = incidentId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder assignedUserId(UUID assignedUserId) {
            this.assignedUserId = assignedUserId;
            return this;
        }

        public Builder newSeverity(String newSeverity) {
            this.newSeverity = newSeverity;
            return this;
        }

        public Builder commentId(UUID commentId) {
            this.commentId = commentId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder newStatus(String newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public Builder assignedUserIds(List<UUID> assignedUserIds) {
            this.assignedUserIds = assignedUserIds;
            return this;
        }

        public Builder actorId(UUID actorId) {
            this.actorId = actorId;
            return this;
        }

        public IncidentEvent build() {
            return new IncidentEvent(incidentId, subject, timestamp, assignedUserId,
                    newSeverity, commentId, title, severity, content, newStatus,
                    assignedUserIds, actorId);
        }
    }
}
