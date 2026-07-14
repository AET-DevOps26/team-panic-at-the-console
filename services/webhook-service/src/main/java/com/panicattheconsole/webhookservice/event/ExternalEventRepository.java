package com.panicattheconsole.webhookservice.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalEventRepository extends JpaRepository<ExternalEvent, UUID> {

    Optional<ExternalEvent> findBySourceAndDeliveryId(String source, String deliveryId);

    Page<ExternalEvent> findBySource(String source, Pageable pageable);

    Page<ExternalEvent> findByEventType(String eventType, Pageable pageable);

    Page<ExternalEvent> findBySourceAndEventType(String source, String eventType, Pageable pageable);

    interface SourceLastEvent {
        String getSource();

        Instant getLastReceivedAt();
    }

    /** Receipt time of the newest event per source slug. */
    @Query("""
            select e.source as source, max(e.receivedAt) as lastReceivedAt
            from ExternalEvent e
            group by e.source
            """)
    List<SourceLastEvent> findLastReceivedBySource();

    /**
     * Events whose NATS publish has not succeeded yet, oldest first. The age
     * cutoff keeps the retrier from racing the in-request publish attempt.
     */
    @Query("""
            select e from ExternalEvent e
            where e.publishedAt is null
              and e.publishAttempts < :maxAttempts
              and e.receivedAt < :receivedBefore
            order by e.receivedAt asc
            """)
    List<ExternalEvent> findPendingPublish(
            @Param("maxAttempts") int maxAttempts,
            @Param("receivedBefore") Instant receivedBefore,
            Pageable pageable);
}
