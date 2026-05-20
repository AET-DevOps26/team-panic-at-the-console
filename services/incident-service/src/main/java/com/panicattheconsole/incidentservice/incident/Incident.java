package com.panicattheconsole.incidentservice.incident;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Incident domain entity.
 * Single source of truth for all incident state.
 * Persists AI-generated content (summary, solutions, postmortem).
 * Publishes NATS events on state changes.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Reference to the external event that triggered this incident creation (if auto-created).
     */
    @Column(name = "source_id")
    private UUID sourceId;

    /**
     * Auto-generated or user-provided title.
     */
    @Column(length = 500)
    private String title;

    /**
     * AI-generated summary of incident state. Regenerable on demand.
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * AI-generated severity suggestion. Regenerable on demand.
     */
    @Column(columnDefinition = "TEXT")
    private String severitySuggestion;

    /**
     * AI-generated solution suggestions. Regenerable on demand.
     */
    @Column(columnDefinition = "TEXT")
    private String solutions;

    /**
     * AI-generated postmortem. Only for resolved incidents. Regenerable on demand.
     */
    @Column(columnDefinition = "TEXT")
    private String postmortem;

    /**
     * Assigned responder IDs (stored as JSON).
     */
    @ElementCollection
    @CollectionTable(name = "incident_assigned_users", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "user_id")
    private Set<UUID> assignedUsers = new HashSet<>();

    /**
     * Immutable comments on this incident.
     */
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    // Constructors
    public Incident() {
    }

    public Incident(UUID id, IncidentStatus status, Severity severity, String title) {
        this.id = id;
        this.status = status;
        this.severity = severity;
        this.title = title;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = Instant.now();
    }

    public String getSeveritySuggestion() {
        return severitySuggestion;
    }

    public void setSeveritySuggestion(String severitySuggestion) {
        this.severitySuggestion = severitySuggestion;
        this.updatedAt = Instant.now();
    }

    public String getSolutions() {
        return solutions;
    }

    public void setSolutions(String solutions) {
        this.solutions = solutions;
        this.updatedAt = Instant.now();
    }

    public String getPostmortem() {
        return postmortem;
    }

    public void setPostmortem(String postmortem) {
        this.postmortem = postmortem;
        this.updatedAt = Instant.now();
    }

    public Set<UUID> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(Set<UUID> assignedUsers) {
        this.assignedUsers = assignedUsers;
        this.updatedAt = Instant.now();
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setIncident(this);
        this.updatedAt = Instant.now();
    }
}
