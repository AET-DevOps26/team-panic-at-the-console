package com.panicattheconsole.webhookservice.publish;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.panicattheconsole.webhookservice.config.WebhookProperties;
import com.panicattheconsole.webhookservice.event.ExternalEvent;
import com.panicattheconsole.webhookservice.event.ExternalEventRepository;

/**
 * Outbox-style safety net: External Events are committed before the NATS
 * publish, so a NATS outage (or a crash between commit and publish) leaves
 * rows with {@code publishedAt IS NULL}. This retrier periodically republishes
 * them, oldest first.
 *
 * <p>Consequence: {@code external.event.received} is at-least-once, and the
 * stable {@code sourceId} is the dedup key for subscribers.
 */
@Component
public class PublishRetrier {

    private static final Logger log = LoggerFactory.getLogger(PublishRetrier.class);
    private static final int BATCH_SIZE = 50;

    private final ExternalEventRepository repository;
    private final ExternalEventPublisher publisher;
    private final WebhookProperties properties;
    private final Clock clock;

    PublishRetrier(ExternalEventRepository repository, ExternalEventPublisher publisher,
            WebhookProperties properties, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${webhook.publish.retry-delay-ms:30000}",
            initialDelayString = "${webhook.publish.retry-delay-ms:30000}")
    public void republishPending() {
        Instant receivedBefore = clock.instant().minusMillis(properties.publish().minAgeMs());
        List<ExternalEvent> pending = repository.findPendingPublish(
                properties.publish().maxAttempts(), receivedBefore, PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }
        log.info("Retrying NATS publish for {} pending external event(s)", pending.size());
        pending.forEach(publisher::publish);
    }
}
