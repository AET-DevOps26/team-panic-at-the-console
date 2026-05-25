package com.panicattheconsole.incidentservice.incident;

/**
 * Incident lifecycle states.
 * Transitions: OPEN → INVESTIGATING → RESOLVED
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED;

    public boolean canTransitionTo(IncidentStatus target) {

        return switch (this) {
            case OPEN -> target == INVESTIGATING;
            case INVESTIGATING -> target == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
