package com.panicattheconsole.notificationservice.domain;

/**
 * Category of a notification, derived from the NATS subject that produced it.
 */
public enum NotificationType {
    INCIDENT_CREATED,
    SEVERITY_ESCALATED,
    INCIDENT_RESOLVED,
    COMMENT_ADDED,
    INCIDENT_ASSIGNED
}
