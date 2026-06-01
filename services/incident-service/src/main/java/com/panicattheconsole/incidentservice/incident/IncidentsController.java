package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.UUID;

import org.openapitools.api.IncidentsApi;
import org.openapitools.model.IncidentEvent;
import org.openapitools.model.PostmortemPatch;
import org.openapitools.model.SeverityPatch;
import org.openapitools.model.SolutionsPatch;
import org.openapitools.model.SummaryPatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
class IncidentsController implements IncidentsApi {

    private final IncidentService incidentService;
    private final ObjectMapper objectMapper;

    IncidentsController(IncidentService incidentService, ObjectMapper objectMapper) {
        this.incidentService = incidentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<org.openapitools.model.Incident> getIncident(UUID incidentId) {
        Incident incident = incidentService.getIncident(incidentId);
        return ResponseEntity.ok(IncidentMapper.toApi(incident));
    }

    @Override
    public ResponseEntity<List<IncidentEvent>> listIncidentEvents(UUID incidentId) {
        incidentService.getIncident(incidentId);
        return ResponseEntity.ok(IncidentMapper.emptyEvents());
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
