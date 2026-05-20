package com.panicattheconsole.incidentservice.genai;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.openapitools.api.GenaiApi;
import org.openapitools.model.RegenAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.incidentservice.incident.IncidentService;

@RestController
class GenaiRegenController implements GenaiApi {

    private static final Logger log = LoggerFactory.getLogger(GenaiRegenController.class);

    private final IncidentService incidentService;

    public GenaiRegenController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Override
    public ResponseEntity genaiHealth() {
        log.info("GenAI health endpoint is not implemented by incident-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSummary(UUID incidentId) {
        try {
            incidentService.requestRegeneration(incidentId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SUMMARY));
        } catch (NoSuchElementException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSeverity(UUID incidentId) {
        try {
            incidentService.requestRegeneration(incidentId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SEVERITY_SUGGESTION));
        } catch (NoSuchElementException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSolutions(UUID incidentId) {
        try {
            incidentService.requestRegeneration(incidentId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SOLUTION_SUGGESTIONS));
        } catch (NoSuchElementException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<RegenAccepted> regeneratePostmortem(UUID incidentId) {
        try {
            incidentService.validatePostmortermAllowed(incidentId);
            incidentService.requestRegeneration(incidentId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new RegenAccepted(true, RegenAccepted.TaskEnum.POSTMORTEM));
        } catch (NoSuchElementException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot regenerate postmortem for non-resolved incident: {}", incidentId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
