package com.panicattheconsole.incidentservice.incident;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openapitools.model.RegenAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
        log.info("Creating incident [id={}, severity={}, title={}]", incidentId, severity, title);

        Incident incident = new Incident(incidentId, IncidentStatus.OPEN, severity, title);
        incident.setSourceId(sourceId);

        Incident saved = incidentRepository.save(incident);
        publishAfterCommit("incident.created", createBaseEvent(saved.getId()));

        return saved;
    }


    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));
    }

    /**
     * Timeline entries for genai prompts and internal reads.
     * event-service is not wired yet; synthesize from incident state and comments until then.
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
        }

        Incident saved = incidentRepository.save(incident);
        publishAfterCommit("incident.updated", createBaseEvent(saved.getId()));

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
        event.put("newSeverity", newSeverity.toString());
        publishAfterCommit("incident.severity.escalated", event);

        log.info("Escalated severity [id={}, old={}, new={}]", incidentId, oldSeverity, newSeverity);
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
     * Called by genai-service after incident resolution. Publishes incident.updated event.
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
     * Publishes incident.regen.requested with a task the genai consumer understands.
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
}
