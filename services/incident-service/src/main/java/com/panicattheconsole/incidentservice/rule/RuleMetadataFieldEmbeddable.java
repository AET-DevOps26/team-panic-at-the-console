package com.panicattheconsole.incidentservice.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A labelled value pulled from the matched event into the incident description,
 * e.g. label "Repository" from field {@code payload.repository.full_name}.
 */
@Embeddable
public class RuleMetadataFieldEmbeddable {

    @Column(name = "label", nullable = false, length = 256)
    private String label;

    @Column(name = "field_path", nullable = false, length = 512)
    private String fieldPath;

    protected RuleMetadataFieldEmbeddable() {
        // for JPA
    }

    public RuleMetadataFieldEmbeddable(String label, String fieldPath) {
        this.label = label;
        this.fieldPath = fieldPath;
    }

    public String getLabel() {
        return label;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
