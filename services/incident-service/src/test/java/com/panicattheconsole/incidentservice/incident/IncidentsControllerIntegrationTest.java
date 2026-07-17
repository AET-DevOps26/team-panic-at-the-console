package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.nats.client.Connection;

/**
 * Exercises the real HTTP surface that genai-service calls via the generated
 * Python client:
 * GET incident, GET events, PATCH AI write-back fields.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class IncidentsControllerIntegrationTest {

        static final UUID INCIDENT_ID = UUID.fromString("018e2c5f-1234-7abc-8def-000000000001");

        @Container
        @SuppressWarnings("resource")
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
                        .withDatabaseName("incidents")
                        .withUsername("postgres")
                        .withPassword("postgres");

        @DynamicPropertySource
        static void configureDatasource(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @MockitoBean
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
        void createIncident_returnsCreatedIncident() {
                ResponseEntity<Map> response = rest.postForEntity(
                                incidentUrl("/incidents"),
                                Map.of("title", "New issue", "severity", "SEV3"),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).containsEntry("title", "New issue");
                assertThat(response.getBody()).containsEntry("severity", "SEV3");
        }

        @Test
        void createIncident_persistsProvidedDescription() {
                ResponseEntity<Map> response = rest.postForEntity(
                                incidentUrl("/incidents"),
                                Map.of(
                                                "title", "New issue",
                                                "severity", "SEV3",
                                                "description", "Orders table partially indexed"),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody())
                                .containsEntry("description", "Orders table partially indexed");
        }

        @Test
        void listIncidents_filtersByStatusAndSeverity() {
                ResponseEntity<Map> response = rest.getForEntity(
                                incidentUrl("/incidents?status=open&severity=SEV2"),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("total", 1);
                assertThat(((List<?>) response.getBody().get("items")).get(0))
                                .asInstanceOf(MAP)
                                .containsEntry("severity", "SEV2");
        }

        @Test
        void updateIncidentStatus_changesStatus() {
                ResponseEntity<Map> response = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/status"),
                                HttpMethod.PATCH,
                                new HttpEntity<>(Map.of("status", "investigating")),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("status", "investigating");
        }

        @Test
        void updateIncidentDescription_setsAndClearsDescription() {
                ResponseEntity<Map> setResponse = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/description"),
                                HttpMethod.PATCH,
                                new HttpEntity<>(Map.of("description", "Rollback of v2.4.1 in progress")),
                                Map.class);

                assertThat(setResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(setResponse.getBody())
                                .containsEntry("description", "Rollback of v2.4.1 in progress");

                ResponseEntity<Map> clearResponse = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/description"),
                                HttpMethod.PATCH,
                                new HttpEntity<>(Map.of("description", "")),
                                Map.class);

                assertThat(clearResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(incidentService.getIncident(INCIDENT_ID).getDescription()).isNull();
        }

        @Test
        void escalateIncidentSeverity_updatesSeverity() {
                ResponseEntity<Map> response = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/severity"),
                                HttpMethod.PATCH,
                                new HttpEntity<>(Map.of("severity", "SEV1")),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("severity", "SEV1");
        }

        @Test
        void assignIncident_updatesAssignedUsers() {
                UUID userId = UUID.fromString("018e2c5f-1234-7abc-8def-000000000004");

                ResponseEntity<Map> response = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/assign"),
                                HttpMethod.PATCH,
                                new HttpEntity<>(Map.of("userIds", List.of(userId.toString()))),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).containsEntry("assignedUserIds", List.of(userId.toString()));
                assertThat(incidentService.getIncident(INCIDENT_ID).getAssignedUsers())
                                .contains(userId);
        }

        @Test
        void addComment_createsCommentAndListsIt() {
                UUID authorId = UUID.fromString("018e2c5f-1234-7abc-8def-0000000000aa");
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-User-Id", authorId.toString());

                ResponseEntity<Map> createResponse = rest.postForEntity(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/comments"),
                                new HttpEntity<>(Map.of("text", "Investigating now"), headers),
                                Map.class);

                assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(createResponse.getBody()).containsEntry("incidentId", INCIDENT_ID.toString());
                assertThat(createResponse.getBody()).containsEntry("text", "Investigating now");
                assertThat(createResponse.getBody()).containsEntry("authorId", authorId.toString());

                ResponseEntity<Map> listResponse = rest.getForEntity(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/comments"),
                                Map.class);

                assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(listResponse.getBody()).containsEntry("total", 1);
                assertThat(((List<?>) listResponse.getBody().get("items")).get(0))
                                .asInstanceOf(MAP)
                                .containsEntry("text", "Investigating now");
        }

        @Test
        void addComment_withoutIdentityHeaderReturns400() {
                ResponseEntity<Map> response = rest.postForEntity(
                                incidentUrl("/incidents/" + INCIDENT_ID + "/comments"),
                                Map.of("text", "anonymous comment"),
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void deleteIncident_removesIncidentAndComments() {
                incidentService.addComment(
                                INCIDENT_ID,
                                UUID.fromString("018e2c5f-1234-7abc-8def-0000000000b1"),
                                UUID.fromString("018e2c5f-1234-7abc-8def-0000000000b2"),
                                "note before deletion");

                ResponseEntity<Void> response = rest.exchange(
                                incidentUrl("/incidents/" + INCIDENT_ID),
                                HttpMethod.DELETE,
                                HttpEntity.EMPTY,
                                Void.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                assertThat(incidentRepository.findById(INCIDENT_ID)).isEmpty();
                assertThat(commentRepository.countByIncident_Id(INCIDENT_ID)).isZero();
        }

        @Test
        void deleteIncident_returns404WhenMissing() {
                UUID missing = UUID.fromString("018e2c5f-1234-7abc-8def-000000000099");

                ResponseEntity<Map> response = rest.exchange(
                                incidentUrl("/incidents/" + missing),
                                HttpMethod.DELETE,
                                HttpEntity.EMPTY,
                                Map.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
