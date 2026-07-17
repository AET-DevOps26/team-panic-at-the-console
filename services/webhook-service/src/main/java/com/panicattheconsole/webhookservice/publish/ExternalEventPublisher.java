package com.panicattheconsole.webhookservice.publish;

import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panicattheconsole.webhookservice.config.WebhookProperties;
import com.panicattheconsole.webhookservice.event.ExternalEvent;
import com.panicattheconsole.webhookservice.event.ExternalEventRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.nats.client.Connection;

/**
 * Publishes External Events to NATS as {@code external.event.received}
 * (schema: api/specs/nats/external.event.received.schema.json) and keeps the
 * per-event publish bookkeeping (publishedAt / publishAttempts) that the
 * {@link PublishRetrier} works from.
 */
@Component
public class ExternalEventPublisher {

    public static final String SUBJECT = "external.event.received";

    private static final Logger log = LoggerFactory.getLogger(ExternalEventPublisher.class);
    private static final Duration FLUSH_TIMEOUT = Duration.ofSeconds(2);

    private final NatsConnectionFactory connectionFactory;
    private final ExternalEventRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final int maxAttempts;

    ExternalEventPublisher(NatsConnectionFactory connectionFactory, ExternalEventRepository repository,
            ObjectMapper objectMapper, MeterRegistry meterRegistry, Clock clock, WebhookProperties properties) {
        this.connectionFactory = connectionFactory;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.maxAttempts = properties.publish().maxAttempts();
    }

    /**
     * Publishes the event and records the outcome on the entity. Failures are
     * swallowed by design: the event is already persisted, and the retrier
     * picks unpublished rows up again.
     */
    public void publish(ExternalEvent event) {
        try {
            byte[] message = buildMessage(event);
            Connection connection = connectionFactory.get();
            connection.publish(SUBJECT, message);
            // publish() only buffers; flush confirms the server received it
            // before the event is marked published.
            connection.flush(FLUSH_TIMEOUT);
            event.markPublished(clock.instant());
            meterRegistry.counter("nats.messages", "subject", SUBJECT, "outcome", "success").increment();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(event, e);
        } catch (Exception e) {
            recordFailure(event, e);
        }
        repository.save(event);
    }

    private void recordFailure(ExternalEvent event, Exception cause) {
        event.recordPublishFailure();
        meterRegistry.counter("nats.messages", "subject", SUBJECT, "outcome", "error").increment();
        if (event.getPublishAttempts() >= maxAttempts) {
            log.error("Giving up publishing external event {} after {} attempts",
                    event.getId(), event.getPublishAttempts(), cause);
        } else {
            log.warn("Failed to publish external event {} (attempt {}), will retry: {}",
                    event.getId(), event.getPublishAttempts(), cause.toString());
        }
    }

    private byte[] buildMessage(ExternalEvent event) throws Exception {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("sourceId", event.getId().toString());
        message.put("source", event.getSource());
        message.put("eventType", event.getEventType());
        message.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(event.getReceivedAt()));
        message.set("rawPayload", objectMapper.readTree(event.getRawPayload()));
        return objectMapper.writeValueAsBytes(message);
    }
}
