package com.panicattheconsole.incidentservice.nats;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.incidentservice.incident.IncidentService;
import com.panicattheconsole.incidentservice.incident.Severity;
import com.panicattheconsole.incidentservice.nats.dto.IncidentCreateRequestedEvent;
import com.panicattheconsole.incidentservice.nats.dto.IncidentSeverityEscalateRequestedEvent;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

@Component
public class NatsSubscriber {

    private static final Logger log = LoggerFactory.getLogger(NatsSubscriber.class);

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final IncidentService incidentService;

    private Dispatcher dispatcher;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public NatsSubscriber(Connection natsConnection, ObjectMapper objectMapper, IncidentService incidentService) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
        this.incidentService = incidentService;
    }

    @PostConstruct
    public void start() {
        if (natsConnection == null) {
            log.warn("NATS connection bean is null; NATS consumers will not start (service in degraded mode)");
            return;
        }

        try {
            dispatcher = natsConnection.createDispatcher((msg) -> {
                // offload processing to executor to avoid blocking NATS IO
                executor.submit(() -> handleMessage(msg));
            });

            dispatcher.subscribe("incident.create.requested");
            dispatcher.subscribe("incident.severity.escalate.requested");

            log.info(
                    "NATS subscribers registered");
        } catch (Exception e) {
            log.error("Failed to initialize NATS subscribers", e);
            // Subscriber initialization failed but don't crash - service can still operate
        }
    }

    private void handleMessage(Message msg) {
        String subject = msg.getSubject();
        String payload = new String(msg.getData(), StandardCharsets.UTF_8);
        try {
            switch (subject) {
                case "incident.create.requested":
                    handleCreateRequested(payload);
                    break;
                case "incident.severity.escalate.requested":
                    handleSeverityEscalationRequested(payload);
                    break;
                default:
                    log.warn("Unhandled subject: {}", subject);
            }
        } catch (Exception e) {
            log.error("Failed to handle NATS message [subject={}, payload={}]", subject, payload, e);
        }
    }

    private void handleCreateRequested(String payload) throws Exception {
        IncidentCreateRequestedEvent evt = objectMapper.readValue(payload, IncidentCreateRequestedEvent.class);

        if (evt.getSourceId() == null || evt.getSeverity() == null) {
            log.warn("Ignoring invalid incident.create.requested event: missing fields");
            return;
        }

        UUID sourceUuid;
        try {
            sourceUuid = UUID.fromString(evt.getSourceId());
        } catch (Exception ex) {
            log.warn("Invalid sourceId UUID in event, ignoring: {}", evt.getSourceId());
            return;
        }

        Severity severity;
        try {
            severity = Severity.valueOf(evt.getSeverity());
        } catch (Exception ex) {
            log.warn("Unknown severity in event, ignoring: {}", evt.getSeverity());
            return;
        }

        UUID incidentId = UUID.randomUUID();
        String title = "Created from external event"; // could be enhanced to allow passing a title in the event

        incidentService.createIncident(incidentId, severity, title, sourceUuid);
        log.info("Processed incident.create.requested -> created incident {}", incidentId);
    }

    private void handleSeverityEscalationRequested(String payload) throws Exception {
        IncidentSeverityEscalateRequestedEvent evt = objectMapper.readValue(payload,
                IncidentSeverityEscalateRequestedEvent.class);

        if (evt.getIncidentId() == null || evt.getRequestedSeverity() == null) {
            log.warn("Ignoring invalid incident.severity.escalate.requested event: missing fields");
            return;
        }

        UUID incidentId;
        try {
            incidentId = UUID.fromString(evt.getIncidentId());
        } catch (Exception ex) {
            log.warn("Invalid incidentId UUID in event, ignoring: {}", evt.getIncidentId());
            return;
        }

        Severity newSeverity;
        try {
            newSeverity = Severity.valueOf(evt.getRequestedSeverity());
        } catch (Exception ex) {
            log.warn("Unknown requestedSeverity in event, ignoring: {}", evt.getRequestedSeverity());
            return;
        }

        try {
            incidentService.escalateSeverity(incidentId, newSeverity);
            log.info("Processed incident.severity.escalate.requested -> escalated {} to {}", incidentId, newSeverity);
        } catch (Exception ex) {
            log.error("Failed to escalate severity for incident {}", incidentId, ex);
        }
    }

    @PreDestroy
    public void stop() {
        if (dispatcher != null) {
            try {
                dispatcher.unsubscribe("incident.create.requested");
                dispatcher.unsubscribe("incident.severity.escalate.requested");
            } catch (Exception e) {
                log.warn("Failed to unsubscribe dispatcher", e);
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
