package com.panicattheconsole.incidentservice.incident;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Immutable comment on an incident.
 * Comments cannot be edited or deleted after creation.
 */
@Entity
@Table(name = "incident_comments")
public class Comment {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public Comment() {
    }

    public Comment(UUID id, UUID authorId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    // Getters (all private setters since comments are immutable)
    public UUID getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    protected void setIncident(Incident incident) {
        this.incident = incident;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
