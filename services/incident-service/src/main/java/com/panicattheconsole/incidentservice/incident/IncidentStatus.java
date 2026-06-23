package com.panicattheconsole.incidentservice.incident;

/**
 * Incident lifecycle states.
 * Transitions: OPEN → INVESTIGATING → RESOLVED
 */
public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED;

    public static IncidentStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase()) {
            case "open" -> OPEN;
            case "investigating" -> INVESTIGATING;
            case "resolved" -> RESOLVED;
            default -> throw new IllegalArgumentException("Unexpected value '" + value + "'");
        };
    }

    public boolean canTransitionTo(IncidentStatus target) {

        return switch (this) {
            case OPEN -> target == INVESTIGATING;
            case INVESTIGATING -> target == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
