package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.openapitools.api.RulesApi;
import org.openapitools.model.Rule;
import org.openapitools.model.RuleInput;
import org.openapitools.model.RuleListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Proxies incident-rule management ({@code /rules}) to incident-service. Access
 * is the same session gate as every other API route.
 */
@RestController
class RulesProxyController implements RulesApi {

    private final RestClient incidentServiceClient;

    RulesProxyController(@Qualifier("incidentServiceClient") RestClient incidentServiceClient) {
        this.incidentServiceClient = incidentServiceClient;
    }

    @Override
    public ResponseEntity<RuleListResponse> listRules() {
        return DownstreamProxy.get(incidentServiceClient, "/rules", RuleListResponse.class);
    }

    @Override
    public ResponseEntity<Rule> createRule(RuleInput ruleInput) {
        return DownstreamProxy.post(incidentServiceClient, "/rules", ruleInput, Rule.class);
    }

    @Override
    public ResponseEntity<Rule> getRule(UUID ruleId) {
        return DownstreamProxy.get(incidentServiceClient, "/rules/{ruleId}", Rule.class, ruleId);
    }

    @Override
    public ResponseEntity<Rule> updateRule(UUID ruleId, RuleInput ruleInput) {
        return DownstreamProxy.put(incidentServiceClient, "/rules/{ruleId}", ruleInput, Rule.class, ruleId);
    }

    @Override
    public ResponseEntity<Void> deleteRule(UUID ruleId) {
        return DownstreamProxy.delete(incidentServiceClient, "/rules/{ruleId}", ruleId);
    }
}
