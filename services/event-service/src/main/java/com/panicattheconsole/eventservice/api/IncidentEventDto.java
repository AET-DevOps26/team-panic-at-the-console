package com.panicattheconsole.eventservice.api;

import java.time.Instant;

/**
 * One timeline entry in the public IncidentEvent shape from api/openapi.yaml:
 * {timestamp, type, description}.
 */
public record IncidentEventDto(
        Instant timestamp,
        String type,
        String description) {
}
