package com.panicattheconsole.incidentservice.incident;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.panicattheconsole.incidentservice.nats.NatsEventPublisher;

/**
 * Service layer for incident operations.
 * Handles persistence, state validation, and NATS event publishing.
 */
@Service
@Transactional
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final CommentRepository commentRepository;
    private final NatsEventPublisher natsEventPublisher;

    public IncidentService(IncidentRepository incidentRepository,
                          CommentRepository commentRepository,
                          NatsEventPublisher natsEventPublisher) {
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.natsEventPublisher = natsEventPublisher;
    }

    /**
     * Create a new incident.
     * Publishes incident.created event.
     */
    public Incident createIncident(UUID incidentId, Severity severity, String title, UUID sourceId) {
        log.info("Creating incident [id={}, severity={}, title={}]", incidentId, severity, title);

        Incident incident = new Incident(incidentId, IncidentStatus.OPEN, severity, title);
        incident.setSourceId(sourceId);

        Incident saved = incidentRepository.save(incident);
        natsEventPublisher.publishIncidentCreated(saved.getId());

        return saved;
    }

    /**
     * Get an incident by ID.
     * Throws NoSuchElementException if not found.
     */
    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));
    }

    /**
     * Update incident status.
     * Publishes incident.updated event.
     */
    public Incident updateIncidentStatus(UUID incidentId, IncidentStatus newStatus) {
        Incident incident = getIncident(incidentId);
        IncidentStatus oldStatus = incident.getStatus();

        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(java.time.Instant.now());
        }

        Incident saved = incidentRepository.save(incident);
        natsEventPublisher.publishIncidentUpdated(saved.getId());

        log.info("Updated incident status [id={}, old={}, new={}]", incidentId, oldStatus, newStatus);
        return saved;
    }

    /**
     * Escalate incident severity.
     * Publishes incident.severity.escalated event.
     */
    public Incident escalateSeverity(UUID incidentId, Severity newSeverity) {
        Incident incident = getIncident(incidentId);
        Severity oldSeverity = incident.getSeverity();

        incident.setSeverity(newSeverity);
        Incident saved = incidentRepository.save(incident);

        natsEventPublisher.publishIncidentSeverityEscalated(saved.getId(), newSeverity.toString());

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
        natsEventPublisher.publishIncidentUpdated(incidentId);
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
        natsEventPublisher.publishIncidentUpdated(incidentId);
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
        natsEventPublisher.publishIncidentUpdated(incidentId);
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
        natsEventPublisher.publishIncidentUpdated(incidentId);
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
        incident.addComment(comment);
        Comment saved = commentRepository.save(comment);

        natsEventPublisher.publishIncidentCommentAdded(incidentId, commentId);
        log.info("Added comment to incident [id={}, commentId={}]", incidentId, commentId);

        return saved;
    }

    /**
     * Assign a user to an incident.
     * Publishes incident.assigned event.
     */
    public Incident assignUser(UUID incidentId, UUID userId) {
        Incident incident = getIncident(incidentId);
        incident.getAssignedUsers().add(userId);
        Incident saved = incidentRepository.save(incident);

        natsEventPublisher.publishIncidentAssigned(incidentId, userId);
        log.info("Assigned user to incident [id={}, userId={}]", incidentId, userId);

        return saved;
    }

    /**
     * Trigger on-demand regeneration of incident AI content.
     * Publishes incident.regen.requested event.
     * Validates that the incident exists.
     */
    public void requestRegeneration(UUID incidentId) {
        // Validate incident exists
        getIncident(incidentId);

        natsEventPublisher.publishIncidentRegenRequested(incidentId);
        log.info("Requested AI regeneration [id={}]", incidentId);
    }

    /**
     * Validate that postmortem regeneration is allowed.
     * Throws IllegalStateException if incident is not resolved.
     */
    public void validatePostmortermAllowed(UUID incidentId) {
        Incident incident = getIncident(incidentId);
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cannot regenerate postmortem for non-resolved incident: " + incidentId);
        }
    }
}
