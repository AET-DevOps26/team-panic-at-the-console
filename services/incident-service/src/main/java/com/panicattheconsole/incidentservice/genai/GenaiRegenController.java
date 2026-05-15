package com.panicattheconsole.incidentservice.genai;

import org.openapitools.api.GenaiApi;
import org.openapitools.model.RegenAccepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class GenaiRegenController implements GenaiApi {

    private static final Logger log = LoggerFactory.getLogger(GenaiRegenController.class);

    @Override
    public ResponseEntity genaiHealth() {
        log.info("GenAI health endpoint is not implemented by incident-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSummary(UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SUMMARY, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SUMMARY));
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSeverity(UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SEVERITY_SUGGESTION, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SEVERITY_SUGGESTION));
    }

    @Override
    public ResponseEntity<RegenAccepted> regenerateSolutions(UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SOLUTION_SUGGESTIONS, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new RegenAccepted(true, RegenAccepted.TaskEnum.SOLUTION_SUGGESTIONS));
    }

    @Override
    public ResponseEntity<RegenAccepted> regeneratePostmortem(UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=POSTMORTEM, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new RegenAccepted(true, RegenAccepted.TaskEnum.POSTMORTEM));
    }
}
