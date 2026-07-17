package com.panicattheconsole.incidentservice.rule;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleMatchDedupRepository extends JpaRepository<RuleMatchDedup, UUID> {

    boolean existsByRuleIdAndDedupKey(UUID ruleId, String dedupKey);
}
