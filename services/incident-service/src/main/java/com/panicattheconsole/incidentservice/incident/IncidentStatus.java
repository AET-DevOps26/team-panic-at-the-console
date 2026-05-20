package com.panicattheconsole.incidentservice.incident;

/**
 * Incident lifecycle states.
 * Transitions: OPEN → INVESTIGATING → RESOLVED
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED
}
