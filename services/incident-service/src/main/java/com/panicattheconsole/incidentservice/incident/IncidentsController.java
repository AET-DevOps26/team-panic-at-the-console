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
import org.openapitools.model.PostmortemPatch;
import org.openapitools.model.SeverityPatch;
import org.openapitools.model.SolutionsPatch;
import org.openapitools.model.SummaryPatch;
import org.openapitools.model.UpdateStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
class IncidentsController implements IncidentsApi {

    private static final Logger log = LoggerFactory.getLogger(IncidentsController.class);

    private final IncidentService incidentService;
    private final ObjectMapper objectMapper;

    IncidentsController(IncidentService incidentService, ObjectMapper objectMapper) {
        this.incidentService = incidentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> createIncident(CreateIncidentRequest createIncidentRequest) {
        UUID incidentId = UUID.randomUUID();
        Severity severity = Severity.valueOf(createIncidentRequest.getSeverity().getValue());
        Incident incident = incidentService.createIncident(incidentId, severity, createIncidentRequest.getTitle(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<IncidentListResponse> listIncidents(org.openapitools.model.IncidentStatus status, 
                                                               org.openapitools.model.Severity severity, 
                                                               Integer limit, Integer offset) {
        int limitVal = limit != null ? limit : 50;
        int offsetVal = offset != null ? offset : 0;

        IncidentStatus statusEnum = status != null ? IncidentStatus.valueOf(status.getValue()) : null;
        Severity severityEnum = severity != null ? Severity.valueOf(severity.getValue()) : null;

        List<Incident> incidents = incidentService.listIncidents(statusEnum, severityEnum, limitVal, offsetVal);
        long total = incidentService.countIncidents(statusEnum, severityEnum);

        IncidentListResponse response = new IncidentListResponse();
        response.setItems(incidents.stream().map(IncidentMapper::toApi).toList());
        response.setTotal((int) total);
        response.setLimit(limitVal);
        response.setOffset(offsetVal);

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
    public ResponseEntity<org.openapitools.model.Incident> updateIncidentStatus(UUID incidentId, UpdateStatusRequest updateStatusRequest) {
        IncidentStatus status = IncidentStatus.valueOf(updateStatusRequest.getStatus().getValue());
        Incident incident = incidentService.updateIncidentStatus(incidentId, status);
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> escalateIncidentSeverity(UUID incidentId, EscalateSeverityRequest escalateSeverityRequest) {
        Severity severity = Severity.valueOf(escalateSeverityRequest.getSeverity().getValue());
        Incident incident = incidentService.escalateSeverity(incidentId, severity);
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> assignIncident(UUID incidentId, AssignIncidentRequest assignIncidentRequest) {
        Incident incident = incidentService.updateAssignedUsers(incidentId, new HashSet<>(assignIncidentRequest.getUserIds()));
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<Comment> addComment(UUID incidentId, CreateCommentRequest createCommentRequest) {
        // Note: In real implementation, we'd get userId from request context (X-User-Id header)
        // For now, generate a placeholder
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID(); // TODO: Extract from security context

        com.panicattheconsole.incidentservice.incident.Comment comment = 
            incidentService.addComment(incidentId, commentId, authorId, createCommentRequest.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentMapper.commentToApi(comment));
    }

    @Override
    public ResponseEntity<CommentListResponse> listComments(UUID incidentId, Integer limit, Integer offset) {
        int limitVal = limit != null ? limit : 50;
        int offsetVal = offset != null ? offset : 0;

        List<com.panicattheconsole.incidentservice.incident.Comment> comments = 
            incidentService.listComments(incidentId, limitVal, offsetVal);
        long total = incidentService.countComments(incidentId);

        CommentListResponse response = new CommentListResponse();
        response.setItems(comments.stream().map(IncidentMapper::commentToApi).toList());
        response.setTotal((int) total);
        response.setLimit(limitVal);
        response.setOffset(offsetVal);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> writeIncidentSummary(UUID incidentId, SummaryPatch summaryPatch) {
        incidentService.updateSummary(incidentId, summaryPatch.getSummary());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> writeIncidentSeveritySuggestion(UUID incidentId, SeverityPatch severityPatch) {
        incidentService.updateSeveritySuggestion(incidentId, serialize(severityPatch));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> writeIncidentSolutions(UUID incidentId, SolutionsPatch solutionsPatch) {
        incidentService.updateSolutions(incidentId, serialize(solutionsPatch.getSolutions()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> writeIncidentPostmortem(UUID incidentId, PostmortemPatch postmortemPatch) {
        incidentService.updatePostmortem(incidentId, serialize(postmortemPatch));
        return ResponseEntity.noContent().build();
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize patch payload", e);
        }
    }
}
