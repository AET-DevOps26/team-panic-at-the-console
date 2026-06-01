package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByIncident_IdOrderByCreatedAtAsc(UUID incidentId);
}
