package com.panicattheconsole.incidentservice.rule;

import java.util.UUID;

import org.openapitools.api.RulesApi;
import org.openapitools.model.Rule;
import org.openapitools.model.RuleInput;
import org.openapitools.model.RuleListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the {@code /rules} CRUD operations from the OpenAPI spec. Access is
 * the same session gate as every other API route; no extra role is required.
 */
@RestController
class RulesController implements RulesApi {

    private final RuleService ruleService;

    RulesController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @Override
    public ResponseEntity<RuleListResponse> listRules() {
        return ResponseEntity.ok(new RuleListResponse(ruleService.listRules()));
    }

    @Override
    public ResponseEntity<Rule> createRule(RuleInput ruleInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(ruleInput));
    }

    @Override
    public ResponseEntity<Rule> getRule(UUID ruleId) {
        return ResponseEntity.ok(ruleService.getRule(ruleId));
    }

    @Override
    public ResponseEntity<Rule> updateRule(UUID ruleId, RuleInput ruleInput) {
        return ResponseEntity.ok(ruleService.updateRule(ruleId, ruleInput));
    }

    @Override
    public ResponseEntity<Void> deleteRule(UUID ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
