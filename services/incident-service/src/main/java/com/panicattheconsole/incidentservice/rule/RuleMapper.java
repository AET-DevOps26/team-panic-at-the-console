package com.panicattheconsole.incidentservice.rule;

import java.time.ZoneOffset;
import java.util.List;

import org.openapitools.model.RuleInput;
import org.openapitools.model.RuleOperator;

import com.panicattheconsole.incidentservice.incident.Severity;

/**
 * Converts between the {@link Rule} entity and the generated OpenAPI models,
 * and validates a {@link RuleInput} before it is applied to an entity.
 */
final class RuleMapper {

    private RuleMapper() {
    }

    /** Copies a validated input onto an entity (create or replace). */
    static void apply(Rule rule, RuleInput input) {
        validate(input);
        rule.setName(input.getName().trim());
        rule.setEnabled(input.getEnabled() == null || input.getEnabled());
        rule.setPriority(input.getPriority() == null ? 100 : input.getPriority());
        rule.setSource(blankToNull(input.getSource()));
        rule.setSeverity(Severity.valueOf(input.getSeverity().getValue()));
        rule.setTitleTemplate(input.getTitleTemplate().trim());
        rule.setDescriptionTemplate(blankToNull(input.getDescriptionTemplate()));
        rule.setDedupKeyTemplate(blankToNull(input.getDedupKeyTemplate()));

        rule.setConditions(input.getConditions().stream()
                .map(c -> new RuleConditionEmbeddable(
                        c.getField().trim(),
                        ConditionOperator.valueOf(c.getOperator().name()),
                        blankToNull(c.getValue())))
                .toList());
        rule.setMetadataFields(input.getMetadataFields().stream()
                .map(m -> new RuleMetadataFieldEmbeddable(m.getLabel().trim(), m.getField().trim()))
                .toList());
    }

    static org.openapitools.model.Rule toApi(Rule rule) {
        org.openapitools.model.Rule api = new org.openapitools.model.Rule();
        api.setId(rule.getId());
        api.setName(rule.getName());
        api.setEnabled(rule.isEnabled());
        api.setPriority(rule.getPriority());
        api.setSource(rule.getSource());
        api.setSeverity(org.openapitools.model.Severity.fromValue(rule.getSeverity().name()));
        api.setTitleTemplate(rule.getTitleTemplate());
        api.setDescriptionTemplate(rule.getDescriptionTemplate());
        api.setDedupKeyTemplate(rule.getDedupKeyTemplate());
        api.setConditions(rule.getConditions().stream()
                .map(c -> new org.openapitools.model.RuleCondition(
                        c.getFieldPath(), RuleOperator.valueOf(c.getOperator().name()))
                        .value(c.getValue()))
                .toList());
        api.setMetadataFields(rule.getMetadataFields().stream()
                .map(m -> new org.openapitools.model.RuleMetadataField(m.getLabel(), m.getFieldPath()))
                .toList());
        api.setCreatedAt(rule.getCreatedAt().atOffset(ZoneOffset.UTC));
        api.setUpdatedAt(rule.getUpdatedAt().atOffset(ZoneOffset.UTC));
        return api;
    }

    static List<org.openapitools.model.Rule> toApi(List<Rule> rules) {
        return rules.stream().map(RuleMapper::toApi).toList();
    }

    private static void validate(RuleInput input) {
        require(input.getName() != null && !input.getName().isBlank(), "name is required");
        require(input.getSeverity() != null, "severity is required");
        require(input.getTitleTemplate() != null && !input.getTitleTemplate().isBlank(),
                "titleTemplate is required");

        if (input.getConditions() != null) {
            for (var condition : input.getConditions()) {
                require(condition.getField() != null && !condition.getField().isBlank(),
                        "every condition needs a field");
                require(condition.getOperator() != null, "every condition needs an operator");
                ConditionOperator operator = ConditionOperator.valueOf(condition.getOperator().name());
                require(!operator.requiresValue()
                        || (condition.getValue() != null && !condition.getValue().isBlank()),
                        "condition operator " + condition.getOperator().getValue() + " requires a value");
            }
        }
        if (input.getMetadataFields() != null) {
            for (var field : input.getMetadataFields()) {
                require(field.getLabel() != null && !field.getLabel().isBlank(),
                        "every metadata field needs a label");
                require(field.getField() != null && !field.getField().isBlank(),
                        "every metadata field needs a field path");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
