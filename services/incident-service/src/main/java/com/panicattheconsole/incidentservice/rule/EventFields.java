package com.panicattheconsole.incidentservice.rule;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolves dotted field paths against an event root and renders
 * {@code {{path}}} templates. The root is an object exposing {@code source},
 * {@code eventType} and {@code payload} (the raw webhook body), so a path like
 * {@code payload.workflow_run.conclusion} walks into the payload.
 *
 * <p>Numeric segments index into arrays ({@code payload.commits.0.id}).
 */
final class EventFields {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    private EventFields() {
    }

    /** The node at {@code path}, or empty if any segment is missing. */
    static Optional<JsonNode> resolve(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return Optional.empty();
        }
        JsonNode current = root;
        for (String rawSegment : path.split("\\.")) {
            String segment = rawSegment.trim();
            if (segment.isEmpty() || current == null || current.isMissingNode() || current.isNull()) {
                return Optional.empty();
            }
            if (current.isArray() && segment.matches("\\d+")) {
                current = current.get(Integer.parseInt(segment));
            } else {
                current = current.get(segment);
            }
        }
        return current == null || current.isMissingNode() ? Optional.empty() : Optional.of(current);
    }

    /**
     * Scalar string at {@code path}, or null if missing/JSON-null. Objects and
     * arrays are serialised compactly so they can still feed comparisons.
     */
    static String scalar(JsonNode root, String path) {
        return resolve(root, path).map(EventFields::scalarString).orElse(null);
    }

    private static String scalarString(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    /** Replaces every {@code {{path}}} with its scalar value; missing -> empty. */
    static String render(String template, JsonNode root) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = scalar(root, matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
