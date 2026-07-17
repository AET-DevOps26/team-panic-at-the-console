package com.panicattheconsole.incidentservice.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One AND-ed match condition of a {@link Rule}: the dotted field path, the
 * comparison operator, and (for value operators) the comparison value.
 */
@Embeddable
public class RuleConditionEmbeddable {

    @Column(name = "field_path", nullable = false, length = 512)
    private String fieldPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 32)
    private ConditionOperator operator;

    @Column(name = "match_value", length = 2048)
    private String value;

    protected RuleConditionEmbeddable() {
        // for JPA
    }

    public RuleConditionEmbeddable(String fieldPath, ConditionOperator operator, String value) {
        this.fieldPath = fieldPath;
        this.operator = operator;
        this.value = value;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}
