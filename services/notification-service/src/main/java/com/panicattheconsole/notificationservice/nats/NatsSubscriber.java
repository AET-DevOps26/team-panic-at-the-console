package com.panicattheconsole.notificationservice.nats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.service.NotificationService;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;

/**
 * Owns the service's NATS lifecycle: it opens a single connection on startup,
 * subscribes to the incident lifecycle subjects notification-service reacts to,
 * and closes the connection on shutdown. notification-service is a pure consumer
 * (it never publishes), so the connection lives here rather than as a shared bean.
 *
 * <p>If NATS is unavailable at startup the service degrades gracefully: the REST
 * API keeps serving already-stored notifications and no events are consumed. Set
 * {@code nats.failOnStartup=true} to abort startup instead.
 *
 * <p>Deliberately does not subscribe to {@code incident.updated}, which carries no
 * notification-worthy change for users, nor to {@code incident.resolved}: resolution
 * also arrives as {@code incident.status.changed} with {@code newStatus=resolved},
 * and consuming both would notify twice.
 */
@Component
public class NatsSubscriber {

    private static final Logger log = LoggerFactory.getLogger(NatsSubscriber.class);

    private static final String INCIDENT_ASSIGNED = "incident.assigned";
    private static final String SEVERITY_ESCALATED = "incident.severity.escalated";
    private static final String STATUS_CHANGED = "incident.status.changed";

    private static final List<String> SUBJECTS = List.of(
            "incident.created",
            SEVERITY_ESCALATED,
            STATUS_CHANGED,
            "incident.comment.added",
            INCIDENT_ASSIGNED);

    private final String natsUrl;
    private final boolean failOnStartup;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    private Connection connection;
    private Dispatcher dispatcher;

    public NatsSubscriber(
            @Value("${nats.url:nats://localhost:4222}") String natsUrl,
            @Value("${nats.failOnStartup:false}") boolean failOnStartup,
            ObjectMapper objectMapper,
            NotificationService notificationService) {
        this.natsUrl = natsUrl;
        this.failOnStartup = failOnStartup;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void start() {
        log.info("Connecting to NATS at {}", natsUrl);
        try {
            connection = Nats.connect(natsUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onStartupFailure(e);
            return;
        } catch (IOException e) {
            onStartupFailure(e);
            return;
        }

        dispatcher = connection.createDispatcher(this::handleMessage);
        SUBJECTS.forEach(dispatcher::subscribe);
        log.info("Subscribed to incident events: {}", SUBJECTS);
    }

    @PreDestroy
    public void stop() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while closing NATS connection", e);
        }
    }

    private void onStartupFailure(Exception cause) {
        if (failOnStartup) {
            throw new IllegalStateException(
                    "Failed to connect to NATS at " + natsUrl + "; startup aborted", cause);
        }
        log.warn("Failed to connect to NATS at {}. Continuing in degraded mode "
                + "(no incident events will be consumed).", natsUrl, cause);
    }

    // Package-private so unit tests can drive it directly without a live NATS connection.
    void handleMessage(Message msg) {
        try {
            String subject = msg.getSubject();

            JsonNode payload = objectMapper.readTree(
                    new String(msg.getData(), StandardCharsets.UTF_8));

            UUID incidentId = readUuid(payload, "incidentId");
            Instant timestamp = readInstant(payload, "timestamp");

            if (incidentId == null || timestamp == null) {
                log.warn("Ignoring event {} because incidentId or timestamp is missing or invalid", subject);
                return;
            }

            UUID assignedUserId = readUuid(payload, "userId");

            if (INCIDENT_ASSIGNED.equals(subject) && assignedUserId == null) {
                log.warn("Ignoring {} because userId is missing or invalid", subject);
                return;
            }

            String newSeverity = readText(payload, "newSeverity");

            if (SEVERITY_ESCALATED.equals(subject) && newSeverity == null) {
                log.warn("Ignoring {} because newSeverity is missing", subject);
                return;
            }

            String newStatus = readText(payload, "newStatus");

            if (STATUS_CHANGED.equals(subject) && newStatus == null) {
                log.warn("Ignoring {} because newStatus is missing", subject);
                return;
            }

            IncidentEvent event = new IncidentEvent.Builder(subject)
                    .incidentId(incidentId)
                    .timestamp(timestamp)
                    .assignedUserId(assignedUserId)
                    .newSeverity(newSeverity)
                    .commentId(readUuid(payload, "commentId"))
                    .title(readText(payload, "title"))
                    .severity(readText(payload, "severity"))
                    .content(readText(payload, "content"))
                    .newStatus(newStatus)
                    .assignedUserIds(readUuidList(payload, "assignedUserIds"))
                    .actorId(readUuid(payload, "actorId"))
                    .build();

            notificationService.record(event);

        } catch (Exception e) {
            log.error("Failed to process NATS message", e);
        }
    }

    private static UUID readUuid(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant readInstant(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Instant.parse(node.asText());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private static String readText(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    /** Parses an array of UUIDs; malformed entries are skipped, a missing field yields null. */
    private static List<UUID> readUuidList(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || !node.isArray()) {
            return null;
        }
        List<UUID> ids = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            try {
                ids.add(UUID.fromString(item.asText()));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping malformed uuid in {}: {}", field, item.asText());
            }
        }
        return ids;
    }
}
