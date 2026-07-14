package com.panicattheconsole.webhookservice.event;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ExternalEventService {

    private final ExternalEventRepository repository;
    private final Clock clock;

    ExternalEventService(ExternalEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record IngestResult(ExternalEvent event, boolean duplicate) {}

    /**
     * Persists a received webhook as a new External Event. Redeliveries
     * (same source and delivery id) return the already-stored event instead,
     * so acknowledging a webhook twice never duplicates it downstream.
     */
    public IngestResult ingest(String source, String deliveryId, String eventType, String rawPayload) {
        if (deliveryId != null) {
            Optional<ExternalEvent> existing = repository.findBySourceAndDeliveryId(source, deliveryId);
            if (existing.isPresent()) {
                return new IngestResult(existing.get(), true);
            }
        }
        ExternalEvent event = new ExternalEvent(
                UUID.randomUUID(), source, eventType, deliveryId, clock.instant(), rawPayload);
        try {
            return new IngestResult(repository.save(event), false);
        } catch (DataIntegrityViolationException e) {
            // Concurrent redelivery hit the (source, delivery_id) unique index
            // between our lookup and insert; the stored row wins.
            return repository.findBySourceAndDeliveryId(source, deliveryId)
                    .map(stored -> new IngestResult(stored, true))
                    .orElseThrow(() -> e);
        }
    }

    public Page<ExternalEvent> list(String source, String eventType, Pageable pageable) {
        if (source != null && eventType != null) {
            return repository.findBySourceAndEventType(source, eventType, pageable);
        }
        if (source != null) {
            return repository.findBySource(source, pageable);
        }
        if (eventType != null) {
            return repository.findByEventType(eventType, pageable);
        }
        return repository.findAll(pageable);
    }

    public Optional<ExternalEvent> get(UUID id) {
        return repository.findById(id);
    }
}
