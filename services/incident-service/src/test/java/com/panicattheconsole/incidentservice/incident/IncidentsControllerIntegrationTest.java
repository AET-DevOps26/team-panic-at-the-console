package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.nats.client.Connection;

/**
 * Exercises the real HTTP surface that genai-service calls via the generated Python client:
 * GET incident, GET events, PATCH AI write-back fields.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IncidentsControllerIntegrationTest {

    static final UUID INCIDENT_ID = UUID.fromString("018e2c5f-1234-7abc-8def-000000000001");

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("incidents")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    Connection natsConnection;

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    IncidentService incidentService;

    @Autowired
    IncidentRepository incidentRepository;

    @Autowired
    CommentRepository commentRepository;

    @BeforeEach
    void seedIncident() {
        commentRepository.deleteAll();
        incidentRepository.deleteAll();
        incidentService.createIncident(INCIDENT_ID, Severity.SEV2, "Checkout 5xx spike", null);
    }

    @Test
    void getIncident_returnsPersistedIncident() {
        ResponseEntity<Map> response = rest.getForEntity(
                incidentUrl("/incidents/" + INCIDENT_ID),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("id", INCIDENT_ID.toString())
                .containsEntry("title", "Checkout 5xx spike")
                .containsEntry("status", "open")
                .containsEntry("severity", "SEV2");
    }

    @Test
    void listIncidentEvents_returnsTimelineWhenIncidentExists() {
        incidentService.addComment(
                INCIDENT_ID,
                UUID.fromString("018e2c5f-1234-7abc-8def-000000000002"),
                UUID.fromString("018e2c5f-1234-7abc-8def-000000000003"),
                "looking into it");

        ResponseEntity<List> response = rest.getForEntity(
                incidentUrl("/incidents/" + INCIDENT_ID + "/events"),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0))
                .asInstanceOf(MAP)
                .containsEntry("type", "incident_created")
                .containsEntry("description", "Incident created: Checkout 5xx spike (SEV2)");
        assertThat(response.getBody().get(1))
                .asInstanceOf(MAP)
                .containsEntry("type", "comment_added")
                .containsEntry("description", "looking into it");
    }

    @Test
    void writeIncidentSummary_persistsSummary() {
        ResponseEntity<Void> patch = rest.exchange(
                incidentUrl("/incidents/" + INCIDENT_ID + "/genai/summary/result"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("summary", "Payment service latency elevated")),
                Void.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(incidentService.getIncident(INCIDENT_ID).getSummary())
                .isEqualTo("Payment service latency elevated");
    }

    @Test
    void writeIncidentSolutions_persistsSolutions() {
        ResponseEntity<Void> patch = rest.exchange(
                incidentUrl("/incidents/" + INCIDENT_ID + "/genai/solutions/result"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "solutions", List.of("Scale checkout replicas", "Roll back deploy"))),
                Void.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(incidentService.getIncident(INCIDENT_ID).getSolutions())
                .contains("Scale checkout replicas");
    }

    @Test
    void writeIncidentSeveritySuggestion_persistsSuggestion() {
        ResponseEntity<Void> patch = rest.exchange(
                incidentUrl("/incidents/" + INCIDENT_ID + "/genai/severity/result"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "severity", "SEV1",
                        "reason", "Customer-facing checkout degraded")),
                Void.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(incidentService.getIncident(INCIDENT_ID).getSeveritySuggestion())
                .contains("SEV1")
                .contains("Customer-facing checkout degraded");
    }

    @Test
    void writeIncidentPostmortem_returns409WhenIncidentNotResolved() {
        ResponseEntity<Map> response = rest.exchange(
                incidentUrl("/incidents/" + INCIDENT_ID + "/genai/postmortem/result"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "rootCause", "misconfigured pool",
                        "timeline", List.of("14:02 deploy"),
                        "actionItems", List.of("add checklist item"))),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void writeIncidentPostmortem_persistsWhenIncidentResolved() {
        incidentService.updateIncidentStatus(INCIDENT_ID, IncidentStatus.INVESTIGATING);
        incidentService.updateIncidentStatus(INCIDENT_ID, IncidentStatus.RESOLVED);

        ResponseEntity<Void> patch = rest.exchange(
                incidentUrl("/incidents/" + INCIDENT_ID + "/genai/postmortem/result"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of(
                        "rootCause", "misconfigured pool",
                        "timeline", List.of("14:02 deploy"),
                        "actionItems", List.of("add checklist item"))),
                Void.class);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(incidentService.getIncident(INCIDENT_ID).getPostmortem())
                .contains("misconfigured pool");
    }

    @Test
    void getIncident_returns404WhenMissing() {
        UUID missing = UUID.fromString("018e2c5f-1234-7abc-8def-000000000099");

        ResponseEntity<Map> response = rest.getForEntity(
                incidentUrl("/incidents/" + missing),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String incidentUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
