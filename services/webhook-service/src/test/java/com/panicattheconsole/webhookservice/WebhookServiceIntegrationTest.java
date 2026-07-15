package com.panicattheconsole.webhookservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.webhookservice.event.ExternalEvent;
import com.panicattheconsole.webhookservice.event.ExternalEventRepository;
import com.panicattheconsole.webhookservice.publish.ExternalEventPublisher;
import com.panicattheconsole.webhookservice.publish.NatsConnectionFactory;
import com.panicattheconsole.webhookservice.publish.PublishRetrier;
import com.panicattheconsole.webhookservice.source.WebhookSourceRepository;

import io.nats.client.Connection;

/**
 * Exercises the full ingest pipeline against a real Postgres: signature
 * enforcement, verbatim persistence, delivery-id dedup, the NATS message
 * contract (external.event.received.schema.json), publish-failure recovery via
 * the retrier, and the read API. Only the NATS connection is mocked.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "webhook.secrets.hooked=test-secret",
            // retrier is invoked manually; keep the schedule out of the way
            "webhook.publish.retry-delay-ms=3600000",
            "webhook.publish.min-age-ms=0"
        })
@AutoConfigureTestRestTemplate
@Testcontainers
class WebhookServiceIntegrationTest {

    static final String WORKFLOW_FAILURE_PAYLOAD = """
            {"action":"completed","workflow_run":{"name":"CI","head_branch":"main","conclusion":"failure"}}""";

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("webhooks")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ExternalEventRepository repository;

    @Autowired
    WebhookSourceRepository sourceRepository;

    @Autowired
    PublishRetrier retrier;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    NatsConnectionFactory connectionFactory;

    Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        sourceRepository.deleteAll();
        connection = mock(Connection.class);
        doReturn(connection).when(connectionFactory).get();
    }

    @Test
    void receivesWebhookPersistsAndPublishesSchemaConformingEvent() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-GitHub-Event", "workflow_run");
        headers.set("X-GitHub-Delivery", "delivery-123");

        ResponseEntity<String> response = post("/webhooks/github", WORKFLOW_FAILURE_PAYLOAD, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode receipt = objectMapper.readTree(response.getBody());
        assertThat(receipt.path("source").asText()).isEqualTo("github");
        assertThat(receipt.path("eventType").asText()).isEqualTo("ci_failure");
        assertThat(receipt.path("duplicate").asBoolean()).isFalse();
        UUID id = UUID.fromString(receipt.path("id").asText());

        // stored verbatim, publish bookkeeping updated
        ExternalEvent stored = repository.findById(id).orElseThrow();
        assertThat(stored.getDeliveryId()).isEqualTo("delivery-123");
        assertThat(objectMapper.readTree(stored.getRawPayload()))
                .isEqualTo(objectMapper.readTree(WORKFLOW_FAILURE_PAYLOAD));
        assertThat(stored.getPublishedAt()).isNotNull();

        // NATS message matches api/specs/nats/external.event.received.schema.json
        ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).publish(eq(ExternalEventPublisher.SUBJECT), messageCaptor.capture());
        JsonNode message = objectMapper.readTree(messageCaptor.getValue());
        assertThat(fieldNames(message)).containsExactlyInAnyOrder("sourceId", "eventType", "timestamp", "rawPayload");
        assertThat(message.path("sourceId").asText()).isEqualTo(id.toString());
        assertThat(message.path("eventType").asText()).isEqualTo("ci_failure");
        assertThat(Instant.parse(message.path("timestamp").asText())).isNotNull();
        assertThat(message.path("rawPayload")).isEqualTo(objectMapper.readTree(WORKFLOW_FAILURE_PAYLOAD));

        // custom metrics from CONTEXT.md are exposed for the Prometheus scrape
        String metrics = rest.getForEntity("/actuator/prometheus", String.class).getBody();
        assertThat(metrics).contains("webhooks_received_total");
        assertThat(metrics).contains("source_type=\"github\"");
        assertThat(metrics).contains("nats_messages_total");
    }

    @Test
    void redeliveryIsAcknowledgedButNotDuplicated() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-GitHub-Event", "workflow_run");
        headers.set("X-GitHub-Delivery", "delivery-once");

        JsonNode first = objectMapper.readTree(post("/webhooks/github", WORKFLOW_FAILURE_PAYLOAD, headers).getBody());
        ResponseEntity<String> redelivery = post("/webhooks/github", WORKFLOW_FAILURE_PAYLOAD, headers);

        assertThat(redelivery.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JsonNode second = objectMapper.readTree(redelivery.getBody());
        assertThat(second.path("duplicate").asBoolean()).isTrue();
        assertThat(second.path("id").asText()).isEqualTo(first.path("id").asText());

        assertThat(repository.count()).isEqualTo(1);
        verify(connection).publish(eq(ExternalEventPublisher.SUBJECT), any(byte[].class));
    }

    @Test
    void rejectsInvalidBodies() {
        assertThat(post("/webhooks/github", "not json", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post("/webhooks/github", "[1,2,3]", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post("/webhooks/Bad_Source!", "{}", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repository.count()).isZero();
    }

    @Test
    void enforcesSignatureForConfiguredSources() throws Exception {
        String body = "{\"eventType\":\"alert_fired\"}";

        assertThat(post("/webhooks/hooked", body, new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders wrongSignature = new HttpHeaders();
        wrongSignature.set("X-Hub-Signature-256", "sha256=" + "0".repeat(64));
        assertThat(post("/webhooks/hooked", body, wrongSignature).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repository.count()).isZero();

        HttpHeaders validSignature = new HttpHeaders();
        validSignature.set("X-Hub-Signature-256", sign("test-secret", body));
        ResponseEntity<String> accepted = post("/webhooks/hooked", body, validSignature);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(objectMapper.readTree(accepted.getBody()).path("eventType").asText()).isEqualTo("alert_fired");
    }

    @Test
    void natsOutageDoesNotLoseEventsRetrierRepublishes() throws Exception {
        doThrow(new IOException("nats down")).when(connectionFactory).get();

        ResponseEntity<String> response = post("/webhooks/github", WORKFLOW_FAILURE_PAYLOAD, new HttpHeaders());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID id = UUID.fromString(objectMapper.readTree(response.getBody()).path("id").asText());

        ExternalEvent stored = repository.findById(id).orElseThrow();
        assertThat(stored.getPublishedAt()).isNull();
        assertThat(stored.getPublishAttempts()).isEqualTo(1);

        // NATS comes back; the scheduled retrier picks the event up
        doReturn(connection).when(connectionFactory).get();
        retrier.republishPending();

        ExternalEvent republished = repository.findById(id).orElseThrow();
        assertThat(republished.getPublishedAt()).isNotNull();
        verify(connection).publish(eq(ExternalEventPublisher.SUBJECT), any(byte[].class));
    }

    @Test
    void readApiListsAndFetchesStoredEvents() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-GitHub-Event", "workflow_run");
        UUID id = UUID.fromString(objectMapper
                .readTree(post("/webhooks/github", WORKFLOW_FAILURE_PAYLOAD, headers).getBody())
                .path("id").asText());

        JsonNode list = objectMapper.readTree(
                rest.getForEntity("/external-events?source=github", String.class).getBody());
        assertThat(list.path("total").asLong()).isEqualTo(1);
        JsonNode item = list.path("items").get(0);
        assertThat(item.path("id").asText()).isEqualTo(id.toString());
        assertThat(item.path("eventType").asText()).isEqualTo("ci_failure");
        assertThat(item.has("rawPayload")).isFalse();

        JsonNode filteredOut = objectMapper.readTree(
                rest.getForEntity("/external-events?source=gitlab", String.class).getBody());
        assertThat(filteredOut.path("total").asLong()).isZero();

        ResponseEntity<String> detail = rest.getForEntity("/external-events/" + id, String.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(detail.getBody()).path("rawPayload"))
                .isEqualTo(objectMapper.readTree(WORKFLOW_FAILURE_PAYLOAD));

        assertThat(rest.getForEntity("/external-events/" + UUID.randomUUID(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sourceLifecycleCreateIngestRotateDelete() throws Exception {
        // register: the create response is the only place the secret appears
        ResponseEntity<String> created = post("/webhook-sources", "{\"slug\":\"grafana\"}", new HttpHeaders());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String secret = objectMapper.readTree(created.getBody()).path("secret").asText();
        assertThat(secret).hasSize(64);

        // duplicate slug is a conflict
        assertThat(post("/webhook-sources", "{\"slug\":\"grafana\"}", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/webhook-sources", "{\"slug\":\"Bad Slug!\"}", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // a registered source must sign, even though signatures are optional globally
        String body = "{\"eventType\":\"alert_fired\"}";
        assertThat(post("/webhooks/grafana", body, new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders signed = new HttpHeaders();
        signed.set("X-Hub-Signature-256", sign(secret, body));
        assertThat(post("/webhooks/grafana", body, signed).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // the list shows the source (with lastEventAt) but never the secret
        JsonNode list = objectMapper.readTree(rest.getForEntity("/webhook-sources", String.class).getBody());
        JsonNode item = list.path("items").get(0);
        assertThat(item.path("slug").asText()).isEqualTo("grafana");
        assertThat(item.has("secret")).isFalse();
        assertThat(item.hasNonNull("lastEventAt")).isTrue();

        // rotation: old secret stops working, new one signs successfully
        ResponseEntity<String> rotated =
                post("/webhook-sources/grafana/rotate-secret", null, new HttpHeaders());
        assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newSecret = objectMapper.readTree(rotated.getBody()).path("secret").asText();
        assertThat(newSecret).isNotEqualTo(secret);
        assertThat(post("/webhooks/grafana", body, signed).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders resigned = new HttpHeaders();
        resigned.set("X-Hub-Signature-256", sign(newSecret, body));
        assertThat(post("/webhooks/grafana", body, resigned).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // deletion keeps the audit trail and returns the slug to unregistered behaviour
        long eventsBefore = repository.count();
        assertThat(rest.exchange("/webhook-sources/grafana", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.exchange("/webhook-sources/grafana", HttpMethod.DELETE,
                HttpEntity.EMPTY, Void.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(repository.count()).isEqualTo(eventsBefore);
        // signatures are optional in this test profile, so the slug ingests unverified again
        assertThat(post("/webhooks/grafana", body, new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> response = rest.getForEntity("/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> post(String path, String body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String sign(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
