package com.panicattheconsole.incidentservice.nats;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedExternalEventRepository extends JpaRepository<ProcessedExternalEvent, java.util.UUID> {

    Optional<ProcessedExternalEvent> findByExternalEventId(String externalEventId);
}
