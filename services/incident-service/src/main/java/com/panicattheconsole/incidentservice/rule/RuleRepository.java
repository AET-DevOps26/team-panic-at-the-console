package com.panicattheconsole.incidentservice.rule;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {

    List<Rule> findAllByOrderByPriorityAscCreatedAtAsc();

    List<Rule> findByEnabledTrueOrderByPriorityAscCreatedAtAsc();
}
