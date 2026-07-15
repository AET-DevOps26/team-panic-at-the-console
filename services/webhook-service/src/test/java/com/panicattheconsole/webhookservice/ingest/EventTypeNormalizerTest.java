package com.panicattheconsole.webhookservice.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EventTypeNormalizerTest {

    private final EventTypeNormalizer normalizer = new EventTypeNormalizer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode json(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    void explicitEventTypeFieldWins() {
        JsonNode payload = json("{\"eventType\":\"alert_fired\"}");
        assertThat(normalizer.normalize("workflow_run", "other_type", payload)).isEqualTo("alert_fired");
    }

    @Test
    void eventTypeHeaderBeatsGithubInference() {
        assertThat(normalizer.normalize("workflow_run", "deploy_finished", json("{}")))
                .isEqualTo("deploy_finished");
    }

    @Test
    void failedWorkflowRunMapsToCiFailure() {
        JsonNode payload = json("{\"workflow_run\":{\"conclusion\":\"failure\"}}");
        assertThat(normalizer.normalize("workflow_run", null, payload)).isEqualTo("ci_failure");
    }

    @Test
    void timedOutWorkflowRunMapsToCiFailure() {
        JsonNode payload = json("{\"workflow_run\":{\"conclusion\":\"timed_out\"}}");
        assertThat(normalizer.normalize("workflow_run", null, payload)).isEqualTo("ci_failure");
    }

    @Test
    void successfulWorkflowRunMapsToCiSuccess() {
        JsonNode payload = json("{\"workflow_run\":{\"conclusion\":\"success\"}}");
        assertThat(normalizer.normalize("workflow_run", null, payload)).isEqualTo("ci_success");
    }

    @Test
    void cancelledWorkflowRunMapsToCiCancelled() {
        JsonNode payload = json("{\"workflow_run\":{\"conclusion\":\"cancelled\"}}");
        assertThat(normalizer.normalize("workflow_run", null, payload)).isEqualTo("ci_cancelled");
    }

    @Test
    void inProgressWorkflowRunKeepsGithubPrefix() {
        JsonNode payload = json("{\"action\":\"in_progress\",\"workflow_run\":{}}");
        assertThat(normalizer.normalize("workflow_run", null, payload)).isEqualTo("github.workflow_run");
    }

    @Test
    void otherGithubEventsGetGithubPrefix() {
        assertThat(normalizer.normalize("push", null, json("{}"))).isEqualTo("github.push");
    }

    @Test
    void fallsBackToUnknown() {
        assertThat(normalizer.normalize(null, null, json("{}"))).isEqualTo("unknown");
        assertThat(normalizer.normalize("  ", "", json("{}"))).isEqualTo("unknown");
    }

    @Test
    void sanitizesToLowercaseSlugCharset() {
        JsonNode payload = json("{\"eventType\":\" CI Failure! \"}");
        assertThat(normalizer.normalize(null, null, payload)).isEqualTo("ci_failure_");
    }

    @Test
    void capsOverlongTypesAtColumnLength() {
        String longType = "x".repeat(300);
        JsonNode payload = json("{\"eventType\":\"" + longType + "\"}");
        assertThat(normalizer.normalize(null, null, payload)).hasSize(128);
    }

    @Test
    void ignoresNonTextualEventTypeField() {
        JsonNode payload = json("{\"eventType\":{\"nested\":true}}");
        assertThat(normalizer.normalize(null, null, payload)).isEqualTo("unknown");
    }
}
