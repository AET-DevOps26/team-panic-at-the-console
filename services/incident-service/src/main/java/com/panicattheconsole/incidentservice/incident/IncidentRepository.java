package com.panicattheconsole.incidentservice.incident;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    Page<Incident> findByStatusAndSeverity(IncidentStatus status, Severity severity, Pageable pageable);

    long countByStatus(IncidentStatus status);

    long countBySeverity(Severity severity);

    long countByStatusAndSeverity(IncidentStatus status, Severity severity);
}
