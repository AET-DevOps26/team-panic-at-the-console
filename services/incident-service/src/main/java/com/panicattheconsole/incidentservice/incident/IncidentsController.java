package com.panicattheconsole.incidentservice.incident;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.openapitools.api.IncidentsApi;
import org.openapitools.model.AssignIncidentRequest;
import org.openapitools.model.Comment;
import org.openapitools.model.CommentListResponse;
import org.openapitools.model.CreateCommentRequest;
import org.openapitools.model.CreateIncidentRequest;
import org.openapitools.model.EscalateSeverityRequest;
import org.openapitools.model.IncidentEvent;
import org.openapitools.model.IncidentListResponse;
import org.openapitools.model.UpdateDescriptionRequest;
import org.openapitools.model.UpdateStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
class IncidentsController implements IncidentsApi {

    private static final Logger log = LoggerFactory.getLogger(IncidentsController.class);

    /** Validated session identity injected by the gateway (ADR 0007). */
    static final String USER_ID_HEADER = "X-User-Id";

    private final IncidentService incidentService;
    private final HttpServletRequest httpRequest;

    IncidentsController(IncidentService incidentService, HttpServletRequest httpRequest) {
        this.incidentService = incidentService;
        this.httpRequest = httpRequest;
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> createIncident(CreateIncidentRequest createIncidentRequest) {
        UUID incidentId = UUID.randomUUID();
        Severity severity = Severity.valueOf(createIncidentRequest.getSeverity().getValue());
        Incident incident = incidentService.createIncident(incidentId, severity, createIncidentRequest.getTitle(),
                createIncidentRequest.getDescription(), null, sessionUserIdOrNull());
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<IncidentListResponse> listIncidents(org.openapitools.model.IncidentStatus status,
            org.openapitools.model.Severity severity,
            Integer page, Integer size) {
        int pageVal = page != null ? page : 0;
        int sizeVal = size != null ? size : 50;

        IncidentStatus statusEnum = status != null ? IncidentStatus.fromValue(status.getValue()) : null;
        Severity severityEnum = severity != null ? Severity.valueOf(severity.getValue()) : null;

        List<Incident> incidents = incidentService.listIncidents(statusEnum, severityEnum, pageVal, sizeVal);
        long total = incidentService.countIncidents(statusEnum, severityEnum);

        IncidentListResponse response = new IncidentListResponse();
        response.setItems(incidents.stream().map(IncidentMapper::toApi).toList());
        response.setTotal((int) total);
        response.setPage(pageVal);
        response.setSize(sizeVal);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> getIncident(UUID incidentId) {
        Incident incident = incidentService.getIncident(incidentId);
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<List<IncidentEvent>> listIncidentEvents(UUID incidentId) {
        return ResponseEntity.ok(incidentService.listIncidentEvents(incidentId));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> updateIncidentDescription(UUID incidentId,
            UpdateDescriptionRequest updateDescriptionRequest) {
        Incident incident = incidentService.updateDescription(incidentId, updateDescriptionRequest.getDescription());
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> updateIncidentStatus(UUID incidentId,
            UpdateStatusRequest updateStatusRequest) {
        IncidentStatus status = IncidentStatus.fromValue(updateStatusRequest.getStatus().getValue());
        Incident incident = incidentService.updateIncidentStatus(incidentId, status, sessionUserIdOrNull());
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> escalateIncidentSeverity(UUID incidentId,
            EscalateSeverityRequest escalateSeverityRequest) {
        Severity severity = Severity.valueOf(escalateSeverityRequest.getSeverity().getValue());
        Incident incident = incidentService.escalateSeverity(incidentId, severity, sessionUserIdOrNull());
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> assignIncident(UUID incidentId,
            AssignIncidentRequest assignIncidentRequest) {
        Incident incident = incidentService.updateAssignedUsers(incidentId,
                new HashSet<>(assignIncidentRequest.getUserIds()), sessionUserIdOrNull());
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<Comment> addComment(UUID incidentId, CreateCommentRequest createCommentRequest) {
        UUID authorId = sessionUserId();
        UUID commentId = UUID.randomUUID();
        com.panicattheconsole.incidentservice.incident.Comment comment = incidentService.addComment(incidentId,
                commentId, authorId, createCommentRequest.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentMapper.commentToApi(comment));
    }

    private UUID sessionUserId() {
        String header = httpRequest.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing " + USER_ID_HEADER + " header");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + USER_ID_HEADER + " header");
        }
    }

    /**
     * Actor identity for event enrichment: optional, because some callers
     * (service-to-service, tests) reach this API without a session.
     */
    private UUID sessionUserIdOrNull() {
        String header = httpRequest.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ResponseEntity<CommentListResponse> listComments(UUID incidentId, Integer page, Integer size) {
        int pageVal = page != null ? page : 0;
        int sizeVal = size != null ? size : 50;

        List<com.panicattheconsole.incidentservice.incident.Comment> comments = incidentService.listComments(incidentId,
                pageVal, sizeVal);
        long total = incidentService.countComments(incidentId);

        CommentListResponse response = new CommentListResponse();
        response.setItems(comments.stream().map(IncidentMapper::commentToApi).toList());
        response.setTotal((int) total);
        response.setSize(sizeVal);
        response.setPage(pageVal);

        return ResponseEntity.ok(response);
    }

}
