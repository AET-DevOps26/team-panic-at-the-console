package com.panicattheconsole.webhookservice.ingest;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Derives the normalised {@code eventType} string of an External Event (e.g.
 * {@code ci_failure}) that Rules are evaluated against. All type-derivation
 * heuristics live here; callers pass the raw request context and get a
 * non-blank, sanitised type back.
 *
 * <p>Precedence: an explicit {@code eventType} field in the payload, then an
 * {@code X-Event-Type} header, then GitHub inference from the
 * {@code X-GitHub-Event} header, then {@code unknown}.
 */
@Component
public class EventTypeNormalizer {

    public static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";
    public static final String EVENT_TYPE_HEADER = "X-Event-Type";

    /** Matches the external_events.event_type column length. */
    private static final int MAX_LENGTH = 128;

    private static final Set<String> FAILED_CONCLUSIONS = Set.of("failure", "timed_out", "startup_failure");

    public String normalize(String githubEventHeader, String eventTypeHeader, JsonNode payload) {
        String explicit = sanitize(payload.path("eventType").asText(null));
        if (explicit != null) {
            return explicit;
        }
        String fromHeader = sanitize(eventTypeHeader);
        if (fromHeader != null) {
            return fromHeader;
        }
        String githubEvent = sanitize(githubEventHeader);
        if (githubEvent != null) {
            return normalizeGithub(githubEvent, payload);
        }
        return "unknown";
    }

    private static String normalizeGithub(String githubEvent, JsonNode payload) {
        if (githubEvent.equals("workflow_run")) {
            String conclusion = payload.path("workflow_run").path("conclusion").asText("");
            if (FAILED_CONCLUSIONS.contains(conclusion)) {
                return "ci_failure";
            }
            if (conclusion.equals("success")) {
                return "ci_success";
            }
            if (conclusion.equals("cancelled")) {
                return "ci_cancelled";
            }
        }
        return truncate("github." + githubEvent);
    }

    /**
     * Event types feed rule conditions and metrics labels, so they are forced
     * into a small charset. Returns null for blank input.
     */
    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_");
        return truncate(cleaned);
    }

    private static String truncate(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
