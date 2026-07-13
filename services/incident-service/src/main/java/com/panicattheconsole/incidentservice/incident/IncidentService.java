package com.panicattheconsole.incidentservice.incident;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.openapitools.model.RegenAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.panicattheconsole.incidentservice.nats.IncidentNatsEvent;

/**
 * Service layer for incident operations.
 * Handles persistence, state validation, and deferred NATS event publishing.
 */
@Service
@Transactional
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public IncidentService(IncidentRepository incidentRepository,
            CommentRepository commentRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    private Map<String, Object> createBaseEvent(UUID incidentId) {
        Map<String, Object> event = new HashMap<>();
        event.put("incidentId", incidentId.toString());
        event.put("timestamp", Instant.now().toString());
        return event;
    }

    private void publishAfterCommit(String subject, Map<String, Object> payload) {
        applicationEventPublisher.publishEvent(new IncidentNatsEvent(subject, payload));
    }

    public Incident createIncident(UUID incidentId, Severity severity, String title, UUID sourceId) {
        return createIncident(incidentId, severity, title, null, sourceId);
    }

    public Incident createIncident(UUID incidentId, Severity severity, String title, String description,
            UUID sourceId) {
        log.info("Creating incident [id={}, severity={}, title={}]", incidentId, severity, title);

        Incident incident = new Incident(incidentId, IncidentStatus.OPEN, severity, title);
        incident.setDescription(description == null || description.isBlank() ? null : description);
        incident.setSourceId(sourceId);

        Incident saved = incidentRepository.save(incident);

        Map<String, Object> event = createBaseEvent(saved.getId());
        if (saved.getTitle() != null) {
            event.put("title", saved.getTitle());
        }
        event.put("severity", saved.getSeverity().toString());
        publishAfterCommit("incident.created", event);

        return saved;
    }

    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));
    }

    /**
     * Timeline entries for genai prompts and internal reads.
     * The public timeline is served by event-service (via the gateway); this
     * synthesized view stays because genai-service calls incident-service
     * directly and needs no cross-service dependency for prompt building.
     */
    public List<org.openapitools.model.IncidentEvent> listIncidentEvents(UUID incidentId) {
        Incident incident = getIncident(incidentId);
        List<Comment> comments = commentRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId);
        return IncidentMapper.toApiEvents(incident, comments);
    }

    public Incident updateIncidentStatus(UUID incidentId, IncidentStatus newStatus) {
        Incident incident = getIncident(incidentId);
        IncidentStatus oldStatus = incident.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + oldStatus + " -> " + newStatus);
        }

        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(java.time.Instant.now());
        } else if (oldStatus == IncidentStatus.RESOLVED) {
            // Reopened: the incident is no longer resolved.
            incident.setResolvedAt(null);
        }

        Incident saved = incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(saved.getId()));

        // Dedicated subject with the transition: incident.updated is too generic
        // for event-service to render a status change in the timeline.
        Map<String, Object> statusEvent = createBaseEvent(saved.getId());
        statusEvent.put("oldStatus", oldStatus.name().toLowerCase());
        statusEvent.put("newStatus", newStatus.name().toLowerCase());
        publishAfterCommit("incident.status.changed", statusEvent);

        if (newStatus == IncidentStatus.RESOLVED) {
            publishAfterCommit("incident.resolved", createBaseEvent(saved.getId()));
        }

        log.info("Updated incident status [id={}, old={}, new={}]", incidentId, oldStatus, newStatus);
        return saved;
    }

    public Incident escalateSeverity(UUID incidentId, Severity newSeverity) {
        Incident incident = getIncident(incidentId);
        Severity oldSeverity = incident.getSeverity();

        incident.setSeverity(newSeverity);
        Incident saved = incidentRepository.save(incident);

        Map<String, Object> event = createBaseEvent(saved.getId());
        event.put("oldSeverity", oldSeverity.toString());
        event.put("newSeverity", newSeverity.toString());
        publishAfterCommit("incident.severity.escalated", event);

        log.info("Escalated severity [id={}, old={}, new={}]", incidentId, oldSeverity, newSeverity);
        return saved;
    }

    /**
     * Update the human-written description.
     * Blank input clears the description. Publishes incident.updated event.
     */
    public Incident updateDescription(UUID incidentId, String description) {
        Incident incident = getIncident(incidentId);
        incident.setDescription(description == null || description.isBlank() ? null : description);
        Incident saved = incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated incident description [id={}]", incidentId);
        return saved;
    }

    /**
     * Update AI-generated summary.
     * Called by genai-service. Publishes incident.updated event.
     */
    public void updateSummary(UUID incidentId, String summary) {
        Incident incident = getIncident(incidentId);
        incident.setSummary(summary);
        incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated incident summary [id={}]", incidentId);
    }

    /**
     * Update AI-generated severity suggestion.
     * Called by genai-service. Publishes incident.updated event.
     */
    public void updateSeveritySuggestion(UUID incidentId, String suggestion) {
        Incident incident = getIncident(incidentId);
        incident.setSeveritySuggestion(suggestion);
        incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated incident severity suggestion [id={}]", incidentId);
    }

    /**
     * Update AI-generated solutions.
     * Called by genai-service. Publishes incident.updated event.
     */
    public void updateSolutions(UUID incidentId, String solutions) {
        Incident incident = getIncident(incidentId);
        incident.setSolutions(solutions);
        incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated incident solutions [id={}]", incidentId);
    }

    /**
     * Update AI-generated postmortem.
     * Called by genai-service after incident resolution. Publishes incident.updated
     * event.
     * Requires incident to be in RESOLVED status.
     */
    public void updatePostmortem(UUID incidentId, String postmortem) {
        Incident incident = getIncident(incidentId);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cannot set postmortem for non-resolved incident: " + incidentId);
        }

        incident.setPostmortem(postmortem);
        incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated incident postmortem [id={}]", incidentId);
    }

    /**
     * Add a comment to an incident.
     * Comments are immutable once created.
     * Publishes incident.comment.added event.
     */
    public Comment addComment(UUID incidentId, UUID commentId, UUID authorId, String content) {
        Incident incident = getIncident(incidentId);

        Comment comment = new Comment(commentId, authorId, content);
        comment.setIncident(incident);
        Comment saved = commentRepository.save(comment);

        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("commentId", commentId.toString());
        // Comments are immutable, so carrying the content lets event-service
        // render it in the timeline without it ever going stale.
        event.put("content", content);
        publishAfterCommit("incident.comment.added", event);
        log.info("Added comment to incident [id={}, commentId={}]", incidentId, commentId);

        return saved;
    }

    public Incident assignUser(UUID incidentId, UUID userId) {
        Incident incident = getIncident(incidentId);
        incident.getAssignedUsers().add(userId);
        Incident saved = incidentRepository.save(incident);

        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("userId", userId.toString());
        publishAfterCommit("incident.assigned", event);
        log.info("Assigned user to incident [id={}, userId={}]", incidentId, userId);

        return saved;
    }

    /**
     * Trigger on-demand regeneration of one AI field.
     * Publishes incident.regen.requested with a task the genai consumer
     * understands.
     */
    public void requestRegeneration(UUID incidentId, RegenAccepted.TaskEnum task) {
        getIncident(incidentId);
        if (task == RegenAccepted.TaskEnum.POSTMORTEM) {
            validatePostmortemAllowed(incidentId);
        }
        publishRegenRequested(incidentId, task);
        log.info("Requested AI regeneration [id={}, task={}]", incidentId, task);
    }

    /**
     * Trigger on-demand regeneration of a postmortem (resolved incidents only).
     */
    public void requestPostmortemRegeneration(UUID incidentId) {
        validatePostmortemAllowed(incidentId);
        publishRegenRequested(incidentId, RegenAccepted.TaskEnum.POSTMORTEM);
        log.info("Requested AI regeneration for postmortem [id={}]", incidentId);
    }

    private void publishRegenRequested(UUID incidentId, RegenAccepted.TaskEnum task) {
        Map<String, Object> event = createBaseEvent(incidentId);
        event.put("task", task.getValue());
        publishAfterCommit("incident.regen.requested", event);
    }

    /**
     * Validate that postmortem regeneration is allowed.
     * Throws IllegalStateException if incident is not resolved.
     */
    public void validatePostmortemAllowed(UUID incidentId) {
        Incident incident = getIncident(incidentId);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cannot regenerate postmortem for non-resolved incident: " + incidentId);
        }
    }

    /**
     * List all incidents with optional filtering by status and severity.
     */
    public List<Incident> listIncidents(
            IncidentStatus status,
            Severity severity,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (status != null && severity != null) {
            return incidentRepository.findByStatusAndSeverity(status, severity, pageable).getContent();
        }

        if (status != null) {
            return incidentRepository.findByStatus(status, pageable).getContent();
        }

        if (severity != null) {
            return incidentRepository.findBySeverity(severity, pageable).getContent();
        }

        return incidentRepository.findAll(pageable).getContent();
    }

    /**
     * Get total count of incidents matching filters.
     */
    public long countIncidents(IncidentStatus status, Severity severity) {
        if (status != null && severity != null) {
            return incidentRepository.countByStatusAndSeverity(status, severity);
        } else if (status != null) {
            return incidentRepository.countByStatus(status);
        } else if (severity != null) {
            return incidentRepository.countBySeverity(severity);
        } else {
            return incidentRepository.count();
        }
    }

    /**
     * Update assigned users for an incident.
     * Replaces the current assignment set with the provided one.
     * Publishes incident.updated event.
     */
    public Incident updateAssignedUsers(UUID incidentId, Set<UUID> userIds) {
        Incident incident = getIncident(incidentId);
        incident.setAssignedUsers(userIds);
        Incident saved = incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(incidentId));
        log.info("Updated assigned users for incident [id={}, count={}]", incidentId, userIds.size());
        return saved;
    }

    /**
     * List comments for an incident.
     */
    public List<Comment> listComments(UUID incidentId, int page, int size) {
        // Validate incident exists
        getIncident(incidentId);
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId, pageable).getContent();
    }

    /**
     * Count comments for an incident.
     */
    public long countComments(UUID incidentId) {
        return commentRepository.countByIncident_Id(incidentId);
    }
}
