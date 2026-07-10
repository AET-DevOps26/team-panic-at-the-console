package com.panicattheconsole.eventservice.api;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One timeline entry in the public IncidentEvent shape from api/openapi.yaml:
 * {timestamp, type, description, newValue?}. newValue carries the new status
 * or severity for change entries and is omitted from the JSON when absent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentEventDto(
        Instant timestamp,
        String type,
        String description,
        String newValue) {
}
