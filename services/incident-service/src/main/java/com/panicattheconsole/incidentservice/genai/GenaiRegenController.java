package com.panicattheconsole.incidentservice.genai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/incidents/{incidentId}/genai")
class GenaiRegenController {

    private static final Logger log = LoggerFactory.getLogger(GenaiRegenController.class);

    record RegenAccepted(boolean accepted, String task) {}

    @PostMapping("/summary")
    ResponseEntity<RegenAccepted> regenerateSummary(@PathVariable("incidentId") UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SUMMARY, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RegenAccepted(true, "SUMMARY"));
    }

    @PostMapping("/severity")
    ResponseEntity<RegenAccepted> regenerateSeverity(@PathVariable("incidentId") UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SEVERITY_SUGGESTION, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RegenAccepted(true, "SEVERITY_SUGGESTION"));
    }

    @PostMapping("/solutions")
    ResponseEntity<RegenAccepted> regenerateSolutions(@PathVariable("incidentId") UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=SOLUTION_SUGGESTIONS, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RegenAccepted(true, "SOLUTION_SUGGESTIONS"));
    }

    @PostMapping("/postmortem")
    ResponseEntity<RegenAccepted> regeneratePostmortem(@PathVariable("incidentId") UUID incidentId) {
        log.info("TODO: publish incident.regen.requested [task=POSTMORTEM, incidentId={}]", incidentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RegenAccepted(true, "POSTMORTEM"));
    }
}
