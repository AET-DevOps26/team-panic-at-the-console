package com.panicattheconsole.gateway.stream;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;

/**
 * Relays incident NATS events to connected SSE clients as small
 * {@code {"type": subject, "incidentId": ...}} envelopes. Clients use them only
 * as cache-invalidation signals; the actual data still flows through the REST
 * proxy routes.
 */
@Component
public class IncidentStreamBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(IncidentStreamBroadcaster.class);

    static final String SUBJECT_WILDCARD = "incident.>";

    // Proxies in front of the gateway (edge nginx, ingress) close idle upstream
    // connections after 60s; periodic comments keep the stream open.
    private static final long HEARTBEAT_INTERVAL_SECONDS = 25;

    private final Connection natsConnection;
    // Own instance: the envelope is plain JSON and the app exposes no ObjectMapper bean.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();

    private Dispatcher dispatcher;

    IncidentStreamBroadcaster(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    @PostConstruct
    void start() {
        if (natsConnection == null) {
            log.warn("NATS connection bean is null; incident stream will stay open but receive no events (degraded mode)");
        } else {
            dispatcher = natsConnection.createDispatcher(msg -> broadcast(msg.getSubject(), msg.getData()));
            dispatcher.subscribe(SUBJECT_WILDCARD);
            log.info("Subscribed to '{}' for SSE fan-out", SUBJECT_WILDCARD);
        }
        heartbeat.scheduleAtFixedRate(
                () -> fanOut(() -> SseEmitter.event().comment("ping")),
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        heartbeat.shutdownNow();
        if (dispatcher != null) {
            try {
                natsConnection.closeDispatcher(dispatcher);
            } catch (Exception e) {
                log.warn("Failed to close NATS dispatcher cleanly", e);
            }
        }
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }

    /** Registers a new SSE client; the emitter is removed again once the connection ends. */
    SseEmitter register() {
        // No server-side timeout: the stream lives as long as the client keeps it open.
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            // Commits the response immediately so the browser fires `onopen`.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (Exception e) {
            emitters.remove(emitter);
            completeQuietly(emitter, e);
        }
        return emitter;
    }

    void broadcast(String subject, byte[] payload) {
        String envelope = toEnvelope(subject, payload);
        fanOut(() -> SseEmitter.event().data(envelope));
    }

    private void fanOut(Supplier<SseEmitter.SseEventBuilder> event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event.get());
            } catch (Exception e) {
                emitters.remove(emitter);
                completeQuietly(emitter, e);
            }
        }
    }

    private String toEnvelope(String subject, byte[] payload) {
        String incidentId = null;
        try {
            JsonNode id = objectMapper.readTree(payload).get("incidentId");
            if (id != null && id.isTextual()) {
                incidentId = id.asText();
            }
        } catch (Exception e) {
            log.debug("Unparsable payload on '{}'; forwarding envelope without incidentId", subject);
        }
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("type", subject);
        envelope.put("incidentId", incidentId);
        return envelope.toString();
    }

    private static void completeQuietly(SseEmitter emitter, Exception cause) {
        try {
            emitter.completeWithError(cause);
        } catch (Exception ignored) {
            // Emitter already completed by the container after the send failure.
        }
    }
}
