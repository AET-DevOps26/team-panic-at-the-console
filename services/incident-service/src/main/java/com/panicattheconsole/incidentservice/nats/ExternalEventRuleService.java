package com.panicattheconsole.incidentservice.nats;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.incidentservice.incident.IncidentService;
import com.panicattheconsole.incidentservice.incident.Severity;

@Service
public class ExternalEventRuleService {

    private static final Logger log = LoggerFactory.getLogger(ExternalEventRuleService.class);

    private final IncidentService incidentService;
    private final ProcessedExternalEventRepository processedExternalEventRepository;
    private final ObjectMapper objectMapper;

    public ExternalEventRuleService(IncidentService incidentService,
            ProcessedExternalEventRepository processedExternalEventRepository,
            ObjectMapper objectMapper) {
        this.incidentService = incidentService;
        this.processedExternalEventRepository = processedExternalEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean shouldCreateIncident(String eventType, String payload) {
        String externalEventId = extractExternalEventId(payload);
        if (externalEventId == null) {
            log.warn("Ignoring external event without a sourceId: {}", payload);
            return false;
        }

        if (processedExternalEventRepository.findByExternalEventId(externalEventId).isPresent()) {
            log.info("Skipping duplicate external event {}", externalEventId);
            return false;
        }

        if (!matchesFailureRule(eventType, payload)) {
            processedExternalEventRepository.save(new ProcessedExternalEvent(externalEventId));
            return false;
        }

        UUID incidentId = UUID.randomUUID();
        String title = "CI failure detected from external event";
        String description = "Webhook event type: " + eventType;

        try {
            UUID eventUuid = UUID.fromString(externalEventId);
            incidentService.createIncident(incidentId, Severity.SEV2, title, description, eventUuid, null);
        } catch (IllegalArgumentException ex) {
            incidentService.createIncident(incidentId, Severity.SEV2, title, description, null, null);
        }

        processedExternalEventRepository.save(new ProcessedExternalEvent(externalEventId));
        log.info("Created incident {} for external event {}", incidentId, externalEventId);
        return true;
    }

    private boolean matchesFailureRule(String eventType, String payload) {
        String normalizedEventType = eventType == null ? "" : eventType.toLowerCase(Locale.ROOT);
        if (normalizedEventType.contains("failure") || normalizedEventType.contains("error")
                || normalizedEventType.contains("failing") || "ci_failure".equals(normalizedEventType)) {
            return true;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            return containsFailureLikeValue(node.get("rawPayload")) || containsFailureLikeValue(node);
        } catch (JsonProcessingException e) {
            log.warn("Could not parse external event payload for failure matching", e);
            return false;
        }
    }

    private boolean containsFailureLikeValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }

        if (node.isTextual()) {
            String value = node.asText().toLowerCase(Locale.ROOT);
            return value.contains("failure") || value.contains("error") || value.contains("failing")
                    || value.contains("failed");
        }

        if (node.isObject()) {
            for (JsonNode child : node) {
                if (containsFailureLikeValue(child)) {
                    return true;
                }
            }
            return false;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsFailureLikeValue(child)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    private String extractExternalEventId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode sourceId = node.get("sourceId");
            return sourceId != null && sourceId.isTextual() ? sourceId.asText() : null;
        } catch (JsonProcessingException e) {
            log.warn("Could not parse external event payload", e);
            return null;
        }
    }
}
