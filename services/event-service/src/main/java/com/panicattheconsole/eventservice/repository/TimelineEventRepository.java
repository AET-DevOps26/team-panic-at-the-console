package com.panicattheconsole.eventservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.panicattheconsole.eventservice.domain.TimelineEvent;

@Repository
public interface TimelineEventRepository
        extends JpaRepository<TimelineEvent, UUID> {

    List<TimelineEvent> findByIncidentIdOrderByEventTimestampAsc(
            UUID incidentId);
}
