package com.panicattheconsole.eventservice.event;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface IncidentEventRepository extends JpaRepository<IncidentEventRecord, Long> {

    List<IncidentEventRecord> findByIncidentIdOrderByTimestampAsc(UUID incidentId);
}
