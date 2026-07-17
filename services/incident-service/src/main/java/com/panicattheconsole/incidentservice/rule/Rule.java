package com.panicattheconsole.incidentservice.rule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.panicattheconsole.incidentservice.incident.Severity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * A configurable rule that turns matching external events into incidents.
 *
 * <p>Rules are evaluated in ascending {@link #priority} order and the first
 * enabled rule whose {@link #source} and every {@link #conditions condition}
 * match creates the incident (first-match-wins). The title and description are
 * rendered from templates against the event; {@link #dedupKeyTemplate} keeps
 * repeated deliveries of the same logical event (e.g. one workflow run) to a
 * single incident.
 */
@Entity
@Table(name = "rules")
public class Rule {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    /** Source slug to scope to; null/blank matches events from any source. */
    @Column(length = 64)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Severity severity;

    @Column(name = "title_template", nullable = false, length = 1024)
    private String titleTemplate;

    @Column(name = "description_template", columnDefinition = "text")
    private String descriptionTemplate;

    @Column(name = "dedup_key_template", length = 1024)
    private String dedupKeyTemplate;

    // @OrderColumn makes these indexed lists (not bags), so two eager
    // collections on one entity do not trip Hibernate's MultipleBagFetch.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rule_conditions", joinColumns = @JoinColumn(name = "rule_id"))
    @OrderColumn(name = "position")
    private List<RuleConditionEmbeddable> conditions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rule_metadata_fields", joinColumns = @JoinColumn(name = "rule_id"))
    @OrderColumn(name = "position")
    private List<RuleMetadataFieldEmbeddable> metadataFields = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Rule() {
        // for JPA
    }

    public Rule(UUID id, Instant now) {
        this.id = id;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public void setDescriptionTemplate(String descriptionTemplate) {
        this.descriptionTemplate = descriptionTemplate;
    }

    public String getDedupKeyTemplate() {
        return dedupKeyTemplate;
    }

    public void setDedupKeyTemplate(String dedupKeyTemplate) {
        this.dedupKeyTemplate = dedupKeyTemplate;
    }

    public List<RuleConditionEmbeddable> getConditions() {
        return conditions;
    }

    public void setConditions(List<RuleConditionEmbeddable> conditions) {
        this.conditions.clear();
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
    }

    public List<RuleMetadataFieldEmbeddable> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<RuleMetadataFieldEmbeddable> metadataFields) {
        this.metadataFields.clear();
        if (metadataFields != null) {
            this.metadataFields.addAll(metadataFields);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
