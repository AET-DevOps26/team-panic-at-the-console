package com.panicattheconsole.incidentservice.incident;

/**
 * Incident lifecycle states.
 * Any transition between distinct states is allowed, including reopening a
 * resolved incident.
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
        return target != null && target != this;
    }
}
