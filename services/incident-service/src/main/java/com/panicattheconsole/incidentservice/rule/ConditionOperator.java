package com.panicattheconsole.incidentservice.rule;

/**
 * How a {@link RuleConditionEmbeddable} compares the value at its field path
 * against the condition value. Mirrors the generated {@code RuleOperator} API
 * enum by constant name so mapping is a straight {@code valueOf}.
 */
public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    MATCHES,
    IN,
    EXISTS,
    NOT_EXISTS;

    /** True when the operator compares against a value (i.e. needs one set). */
    public boolean requiresValue() {
        return this != EXISTS && this != NOT_EXISTS;
    }
}
