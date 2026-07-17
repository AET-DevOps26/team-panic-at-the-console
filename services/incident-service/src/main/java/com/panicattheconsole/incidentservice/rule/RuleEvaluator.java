package com.panicattheconsole.incidentservice.rule;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panicattheconsole.incidentservice.incident.IncidentService;

/**
 * Turns an {@code external.event.received} message into an incident by matching
 * it against configured {@link Rule rules}. Rules run in ascending priority and
 * the first enabled rule whose source and every condition match wins.
 */
@Service
public class RuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluator.class);

    private final RuleService ruleService;
    private final RuleMatchDedupRepository dedupRepository;
    private final IncidentService incidentService;
    private final ObjectMapper objectMapper;

    public RuleEvaluator(RuleService ruleService, RuleMatchDedupRepository dedupRepository,
            IncidentService incidentService, ObjectMapper objectMapper) {
        this.ruleService = ruleService;
        this.dedupRepository = dedupRepository;
        this.incidentService = incidentService;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates the raw NATS message. Returns true if an incident was created.
     */
    @Transactional
    public boolean evaluate(String message) {
        JsonNode envelope;
        try {
            envelope = objectMapper.readTree(message);
        } catch (Exception e) {
            log.warn("Could not parse external event message", e);
            return false;
        }
        if (envelope == null || !envelope.isObject()) {
            log.warn("Ignoring non-object external event message: {}", message);
            return false;
        }

        String source = text(envelope, "source");
        String eventType = text(envelope, "eventType");
        String sourceId = text(envelope, "sourceId");
        JsonNode root = buildRoot(source, eventType, envelope.get("rawPayload"));

        for (Rule rule : ruleService.enabledRulesInEvaluationOrder()) {
            if (!matchesSource(rule, source) || !matchesConditions(rule, root)) {
                continue;
            }
            return createIncident(rule, root, sourceId);
        }
        return false;
    }

    private ObjectNode buildRoot(String source, String eventType, JsonNode rawPayload) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("source", source);
        root.put("eventType", eventType);
        root.set("payload", rawPayload == null ? objectMapper.nullNode() : rawPayload);
        return root;
    }

    private static boolean matchesSource(Rule rule, String source) {
        String ruleSource = rule.getSource();
        if (ruleSource == null || ruleSource.isBlank()) {
            return true;
        }
        return ruleSource.equalsIgnoreCase(source);
    }

    private boolean matchesConditions(Rule rule, JsonNode root) {
        for (RuleConditionEmbeddable condition : rule.getConditions()) {
            if (!matches(condition, root)) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(RuleConditionEmbeddable condition, JsonNode root) {
        Optional<JsonNode> node = EventFields.resolve(root, condition.getFieldPath());
        boolean present = node.isPresent() && !node.get().isNull();
        String actual = EventFields.scalar(root, condition.getFieldPath());
        String value = condition.getValue();

        return switch (condition.getOperator()) {
            case EXISTS -> present;
            case NOT_EXISTS -> !present;
            case EQUALS -> value != null && value.equals(actual);
            case NOT_EQUALS -> value == null || !value.equals(actual);
            case CONTAINS -> actual != null && value != null && actual.contains(value);
            case NOT_CONTAINS -> actual == null || value == null || !actual.contains(value);
            case IN -> actual != null && value != null && Arrays.stream(value.split(","))
                    .map(String::trim)
                    .anyMatch(actual::equals);
            case MATCHES -> actual != null && value != null && regexFind(value, actual);
        };
    }

    private static boolean regexFind(String regex, String actual) {
        try {
            return Pattern.compile(regex).matcher(actual).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex in rule condition, treating as no match: {}", regex);
            return false;
        }
    }

    private boolean createIncident(Rule rule, JsonNode root, String sourceId) {
        String dedupKey = resolveDedupKey(rule, root, sourceId);
        if (dedupRepository.existsByRuleIdAndDedupKey(rule.getId(), dedupKey)) {
            log.info("Skipping duplicate for rule {} dedupKey {}", rule.getId(), dedupKey);
            return false;
        }

        String title = EventFields.render(rule.getTitleTemplate(), root);
        if (title == null || title.isBlank()) {
            title = "Incident from rule: " + rule.getName();
        }
        String description = buildDescription(rule, root);
        UUID incidentId = UUID.randomUUID();

        incidentService.createIncident(incidentId, rule.getSeverity(), title, description,
                parseUuidOrNull(sourceId), null);
        // The unique (rule, dedupKey) constraint is the backstop: if a rare
        // concurrent delivery slipped past the exists() check above, the insert
        // fails and the whole transaction rolls back (no duplicate incident).
        dedupRepository.save(new RuleMatchDedup(rule.getId(), dedupKey));

        log.info("Created incident {} from rule {} ({})", incidentId, rule.getId(), rule.getName());
        return true;
    }

    private String resolveDedupKey(Rule rule, JsonNode root, String sourceId) {
        String template = rule.getDedupKeyTemplate();
        if (template != null && !template.isBlank()) {
            String rendered = EventFields.render(template, root);
            if (rendered != null && !rendered.isBlank()) {
                return "rule:" + rule.getId() + ":" + rendered.trim();
            }
        }
        // No usable business key: fall back to the delivery id so a redelivery of
        // the same event is still deduplicated, but distinct events are not.
        return "event:" + (sourceId == null ? UUID.randomUUID().toString() : sourceId);
    }

    private String buildDescription(Rule rule, JsonNode root) {
        StringBuilder sb = new StringBuilder();
        if (rule.getDescriptionTemplate() != null && !rule.getDescriptionTemplate().isBlank()) {
            sb.append(EventFields.render(rule.getDescriptionTemplate(), root).trim()).append("\n\n");
        }
        for (RuleMetadataFieldEmbeddable field : rule.getMetadataFields()) {
            String value = EventFields.scalar(root, field.getFieldPath());
            sb.append("- **").append(field.getLabel()).append(":** ")
                    .append(value == null || value.isBlank() ? "—" : value).append('\n');
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
