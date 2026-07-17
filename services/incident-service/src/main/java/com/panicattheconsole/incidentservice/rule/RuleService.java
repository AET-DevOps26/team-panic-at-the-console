package com.panicattheconsole.incidentservice.rule;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.openapitools.model.RuleInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for incident rules plus the ordered read the {@link RuleEvaluator} uses.
 * All entity/API mapping happens here so callers only handle generated models.
 */
@Service
@Transactional
public class RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<org.openapitools.model.Rule> listRules() {
        return RuleMapper.toApi(ruleRepository.findAllByOrderByPriorityAscCreatedAtAsc());
    }

    public org.openapitools.model.Rule getRule(UUID id) {
        return RuleMapper.toApi(findOrThrow(id));
    }

    public org.openapitools.model.Rule createRule(RuleInput input) {
        Rule rule = new Rule(UUID.randomUUID(), Instant.now());
        RuleMapper.apply(rule, input);
        Rule saved = ruleRepository.save(rule);
        log.info("Created rule [id={}, name={}]", saved.getId(), saved.getName());
        return RuleMapper.toApi(saved);
    }

    public org.openapitools.model.Rule updateRule(UUID id, RuleInput input) {
        Rule rule = findOrThrow(id);
        RuleMapper.apply(rule, input);
        rule.touch(Instant.now());
        Rule saved = ruleRepository.save(rule);
        log.info("Updated rule [id={}, name={}]", saved.getId(), saved.getName());
        return RuleMapper.toApi(saved);
    }

    public void deleteRule(UUID id) {
        if (!ruleRepository.existsById(id)) {
            throw new NoSuchElementException("Rule not found: " + id);
        }
        ruleRepository.deleteById(id);
        log.info("Deleted rule [id={}]", id);
    }

    /** Enabled rules in evaluation order (ascending priority). */
    @Transactional(readOnly = true)
    public List<Rule> enabledRulesInEvaluationOrder() {
        return ruleRepository.findByEnabledTrueOrderByPriorityAscCreatedAtAsc();
    }

    /**
     * Seeds the GitHub CI-failure rule on first boot so the platform keeps
     * working out of the box. Runs only when no rules exist, so user edits are
     * never overwritten.
     */
    public void seedDefaultRuleIfEmpty() {
        if (ruleRepository.count() > 0) {
            return;
        }
        Rule rule = new Rule(UUID.randomUUID(), Instant.now());
        rule.setName("GitHub CI failures");
        rule.setEnabled(true);
        rule.setPriority(100);
        rule.setSource("github");
        rule.setSeverity(com.panicattheconsole.incidentservice.incident.Severity.SEV2);
        rule.setTitleTemplate("CI failure: {{payload.workflow_run.name}}");
        rule.setDescriptionTemplate(
                "Workflow **{{payload.workflow_run.name}}** failed on branch "
                        + "`{{payload.workflow_run.head_branch}}`.");
        rule.setDedupKeyTemplate("{{payload.workflow_run.id}}");
        rule.setConditions(List.of(
                new RuleConditionEmbeddable("eventType", ConditionOperator.EQUALS, "ci_failure")));
        rule.setMetadataFields(List.of(
                new RuleMetadataFieldEmbeddable("Repository", "payload.repository.full_name"),
                new RuleMetadataFieldEmbeddable("Branch", "payload.workflow_run.head_branch"),
                new RuleMetadataFieldEmbeddable("Commit", "payload.workflow_run.head_sha"),
                new RuleMetadataFieldEmbeddable("Triggered by", "payload.workflow_run.triggering_actor.login"),
                new RuleMetadataFieldEmbeddable("Run attempt", "payload.workflow_run.run_attempt"),
                new RuleMetadataFieldEmbeddable("Run URL", "payload.workflow_run.html_url")));
        ruleRepository.save(rule);
        log.info("Seeded default rule [id={}, name={}]", rule.getId(), rule.getName());
    }

    private Rule findOrThrow(UUID id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + id));
    }
}
