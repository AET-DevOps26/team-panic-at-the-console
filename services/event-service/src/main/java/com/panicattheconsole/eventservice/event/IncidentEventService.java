package com.panicattheconsole.eventservice.event;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openapitools.model.IncidentEvent;

@Service
public class IncidentEventService {

    private final IncidentEventRepository repository;

    public IncidentEventService(IncidentEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID incidentId, Instant timestamp, String type, String description) {
        repository.save(new IncidentEventRecord(incidentId, timestamp, type, description));
    }

    @Transactional(readOnly = true)
    public List<IncidentEvent> listForIncident(UUID incidentId) {
        return repository.findByIncidentIdOrderByTimestampAsc(incidentId).stream()
                .map(r -> new IncidentEvent(
                        r.getTimestamp().atOffset(ZoneOffset.UTC),
                        r.getType(),
                        r.getDescription()))
                .toList();
    }
}
