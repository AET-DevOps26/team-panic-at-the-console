package com.panicattheconsole.incidentservice.nats;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes domain events to NATS only after the surrounding transaction has committed.
 */
@Component
public class NatsEventPublishingListener {

    private final NatsEventPublisher natsEventPublisher;

    public NatsEventPublishingListener(NatsEventPublisher natsEventPublisher) {
        this.natsEventPublisher = natsEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIncidentNatsEvent(IncidentNatsEvent event) {
        natsEventPublisher.publish(event.getSubject(), event.getPayload());
    }
}
