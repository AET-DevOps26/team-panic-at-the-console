package com.panicattheconsole.eventservice.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        UUID incidentId,
        String eventType,
        Instant timestamp,
        JsonNode payload) {
}
