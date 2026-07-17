package com.panicattheconsole.incidentservice.rule;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Idempotency record: at most one incident per (rule, dedup key). The unique
 * constraint is what actually enforces "one incident per workflow run" even if
 * the same event is redelivered under a new external event id.
 */
@Entity
@Table(name = "rule_match_dedup", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rule_match_dedup", columnNames = { "rule_id", "dedup_key" })
})
public class RuleMatchDedup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false, updatable = false)
    private UUID ruleId;

    @Column(name = "dedup_key", nullable = false, updatable = false, length = 1024)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RuleMatchDedup() {
        // for JPA
    }

    public RuleMatchDedup(UUID ruleId, String dedupKey) {
        this.ruleId = ruleId;
        this.dedupKey = dedupKey;
        this.createdAt = Instant.now();
    }
}
