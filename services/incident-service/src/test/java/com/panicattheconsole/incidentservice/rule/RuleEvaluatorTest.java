package com.panicattheconsole.incidentservice.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.incidentservice.incident.IncidentService;
import com.panicattheconsole.incidentservice.incident.Severity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private RuleMatchDedupRepository dedupRepository;

    @Mock
    private IncidentService incidentService;

    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleEvaluator(ruleService, dedupRepository, incidentService, new ObjectMapper());
    }

    private static Rule githubCiFailureRule() {
        Rule rule = new Rule(UUID.randomUUID(), Instant.now());
        rule.setName("GitHub CI failures");
        rule.setEnabled(true);
        rule.setPriority(100);
        rule.setSource("github");
        rule.setSeverity(Severity.SEV2);
        rule.setTitleTemplate("CI failure: {{payload.workflow_run.name}}");
        rule.setDedupKeyTemplate("{{payload.workflow_run.id}}");
        rule.setConditions(List.of(
                new RuleConditionEmbeddable("eventType", ConditionOperator.EQUALS, "ci_failure")));
        rule.setMetadataFields(List.of(
                new RuleMetadataFieldEmbeddable("Repository", "payload.repository.full_name"),
                new RuleMetadataFieldEmbeddable("Branch", "payload.workflow_run.head_branch")));
        return rule;
    }

    private static String message(String eventType, String source) {
        return """
                {
                  "sourceId":"%s",
                  "source":"%s",
                  "eventType":"%s",
                  "timestamp":"2026-07-15T10:00:00Z",
                  "rawPayload":{
                    "action":"completed",
                    "workflow_run":{
                      "id":998877,
                      "name":"Build",
                      "conclusion":"failure",
                      "head_branch":"main"
                    },
                    "repository":{"full_name":"demo/repo"}
                  }
                }
                """.formatted(UUID.randomUUID(), source, eventType);
    }

    @Test
    void ciFailureFromGithub_createsIncidentWithRenderedTitleAndMetadata() {
        when(ruleService.enabledRulesInEvaluationOrder()).thenReturn(List.of(githubCiFailureRule()));
        when(dedupRepository.existsByRuleIdAndDedupKey(any(), any())).thenReturn(false);

        boolean created = evaluator.evaluate(message("ci_failure", "github"));

        assertTrue(created);
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(incidentService).createIncident(any(UUID.class), eq(Severity.SEV2),
                title.capture(), description.capture(), any(), isNull());
        assertThat(title.getValue()).isEqualTo("CI failure: Build");
        assertThat(description.getValue()).contains("Repository").contains("demo/repo").contains("main");
        verify(dedupRepository).save(any(RuleMatchDedup.class));
    }

    @Test
    void ciSuccess_doesNotMatchFailureRule() {
        when(ruleService.enabledRulesInEvaluationOrder()).thenReturn(List.of(githubCiFailureRule()));

        boolean created = evaluator.evaluate(message("ci_success", "github"));

        assertFalse(created);
        verify(incidentService, never()).createIncident(any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateDedupKey_isSkipped() {
        when(ruleService.enabledRulesInEvaluationOrder()).thenReturn(List.of(githubCiFailureRule()));
        when(dedupRepository.existsByRuleIdAndDedupKey(any(), any())).thenReturn(true);

        boolean created = evaluator.evaluate(message("ci_failure", "github"));

        assertFalse(created);
        verify(incidentService, never()).createIncident(any(), any(), any(), any(), any(), any());
    }

    @Test
    void differentSource_doesNotMatchSourceScopedRule() {
        when(ruleService.enabledRulesInEvaluationOrder()).thenReturn(List.of(githubCiFailureRule()));

        boolean created = evaluator.evaluate(message("ci_failure", "gitlab"));

        assertFalse(created);
        verify(incidentService, never()).createIncident(any(), any(), any(), any(), any(), any());
    }
}
