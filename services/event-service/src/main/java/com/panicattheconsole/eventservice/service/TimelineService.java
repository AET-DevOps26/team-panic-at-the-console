package com.panicattheconsole.eventservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.panicattheconsole.eventservice.api.IncidentEventDto;
import com.panicattheconsole.eventservice.domain.TimelineEvent;
import com.panicattheconsole.eventservice.messaging.EventEnvelope;
import com.panicattheconsole.eventservice.repository.TimelineEventRepository;

import jakarta.transaction.Transactional;

@Service
public class TimelineService {

    private final TimelineEventRepository repository;

    public TimelineService(TimelineEventRepository repository) {
        this.repository = repository;
    }

    public List<TimelineEvent> getTimeline(UUID incidentId) {
        return repository.findByIncidentIdOrderByEventTimestampAsc(
                incidentId);
    }

    /** Timeline in the public IncidentEvent shape, served through the gateway. */
    public List<IncidentEventDto> getPublicTimeline(UUID incidentId) {
        return getTimeline(incidentId).stream()
                .map(IncidentEventMapper::toApi)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public void append(EventEnvelope event) {

        TimelineEvent timelineEvent = new TimelineEvent(
                event.incidentId(),
                event.eventType(),
                event.timestamp(),
                event.payload());

        repository.save(timelineEvent);
    }
}
