package com.panicattheconsole.eventservice.nats;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.eventservice.messaging.EventEnvelope;
import com.panicattheconsole.eventservice.service.TimelineService;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

@Component
public class NatsSubscriber {

        private static final Logger log = LoggerFactory.getLogger(NatsSubscriber.class);

        private final Connection natsConnection;
        private final ObjectMapper objectMapper;
        private final TimelineService timelineService;

        private Dispatcher dispatcher;

        public NatsSubscriber(
                        Connection natsConnection,
                        ObjectMapper objectMapper,
                        TimelineService timelineService) {
                this.natsConnection = natsConnection;
                this.objectMapper = objectMapper;
                this.timelineService = timelineService;
        }

        @PostConstruct
        public void start() {

                if (natsConnection == null) {
                        log.warn("NATS connection bean is null; NATS consumers will not start (service in degraded mode)");
                        return;
                }

                dispatcher = natsConnection.createDispatcher(this::handleMessage);

                dispatcher.subscribe("incident.created");
                dispatcher.subscribe("incident.updated");
                dispatcher.subscribe("incident.resolved");
                dispatcher.subscribe("incident.assigned");
                dispatcher.subscribe("incident.comment.added");
                dispatcher.subscribe("incident.severity.escalated");

                log.info("Event service subscribed to incident events");
        }

        private void handleMessage(Message msg) {

                try {

                        String subject = msg.getSubject();

                        JsonNode payload = objectMapper.readTree(
                                        new String(
                                                        msg.getData(),
                                                        StandardCharsets.UTF_8));

                        JsonNode incidentIdNode = payload.get("incidentId");

                        JsonNode timestampNode = payload.get("timestamp");

                        if (incidentIdNode == null || timestampNode == null) {

                                log.warn(
                                                "Ignoring event {} because incidentId or timestamp is missing",
                                                subject);

                                return;
                        }

                        UUID incidentId = UUID.fromString(
                                        incidentIdNode.asText());

                        Instant timestamp = Instant.parse(
                                        timestampNode.asText());

                        EventEnvelope eventEnvelope = new EventEnvelope(
                                        incidentId,
                                        subject,
                                        timestamp,
                                        payload);

                        timelineService.append(eventEnvelope);

                        log.debug(
                                        "Stored timeline event {} for incident {}",
                                        subject,
                                        incidentId);

                } catch (Exception e) {

                        log.error(
                                        "Failed to process NATS message",
                                        e);
                }
        }
}
