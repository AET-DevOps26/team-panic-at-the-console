package com.panicattheconsole.notificationservice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.nats.NatsSubscriber;
import com.panicattheconsole.notificationservice.repository.NotificationReadRepository;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;
import com.panicattheconsole.notificationservice.service.NotificationService;

/**
 * Exercises the per-user visibility and read-state model, unread counts, and
 * the mark-read flows against a real Postgres instance: logic that lives in
 * custom repository queries and cannot be covered by mock-based unit tests.
 *
 * <p>Caller identity arrives via the X-User-Id header, as injected by the
 * gateway in production (ADR 0007).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class NotificationIntegrationTest {

    static final UUID INCIDENT_ID = UUID.fromString("018e2c5f-1234-7abc-8def-000000000001");
    static final UUID USER_A = UUID.fromString("018e2c5f-1234-7abc-8def-0000000000aa");
    static final UUID USER_B = UUID.fromString("018e2c5f-1234-7abc-8def-0000000000bb");
    static final Instant TS = Instant.parse("2026-06-12T12:00:00Z");

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("notifications")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Replace the subscriber so the context boots without opening a real NATS connection;
    // these tests drive the REST + persistence layers directly.
    @MockitoBean
    NatsSubscriber natsSubscriber;

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationReadRepository notificationReadRepository;

    @BeforeEach
    void seed() {
        notificationReadRepository.deleteAll();
        notificationRepository.deleteAll();
        // Machine-created broadcast (no actor): visible to everyone.
        notificationService.record(event("incident.created")
                .title("Checkout down").severity("SEV1").build());
        // Personal assignment notifications for two different users.
        notificationService.record(event("incident.assigned")
                .assignedUserId(USER_A).actorId(USER_B).build());
        notificationService.record(event("incident.assigned")
                .assignedUserId(USER_B).actorId(USER_A).build());
    }

    private static IncidentEvent.Builder event(String subject) {
        return new IncidentEvent.Builder(subject).incidentId(INCIDENT_ID).timestamp(TS);
    }

    @Test
    void listForUser_returnsPersonalAndBroadcast_excludingOtherUsers() {
        ResponseEntity<Map> response = getAs(USER_A, "/notifications");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("total", 2)
                .containsEntry("unreadCount", 2);

        List<?> items = (List<?>) response.getBody().get("items");
        assertThat(items).hasSize(2);
        // Each item is either the broadcast (null recipient) or A's personal one, never B's.
        // Also confirms the generated JsonNullable<UUID> serializes to a plain value or null.
        assertThat(items).allSatisfy(item -> {
            Object recipientId = ((Map<?, ?>) item).get("recipientId");
            if (recipientId != null) {
                assertThat(recipientId).isEqualTo(USER_A.toString());
            }
        });
    }

    @Test
    void broadcastMessage_carriesTitleAndSeverity() {
        Map<?, ?> broadcast = broadcastItemFor(USER_A);
        assertThat(broadcast.get("message")).isEqualTo("New incident: Checkout down (SEV1)");
    }

    @Test
    void missingUserIdHeader_returns400() {
        ResponseEntity<Map> response = rest.getForEntity(url("/notifications"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void broadcastReadByA_staysUnreadForB() {
        UUID broadcastId = UUID.fromString((String) broadcastItemFor(USER_A).get("id"));

        assertThat(postAs(USER_A, "/notifications/" + broadcastId + "/read").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getAs(USER_A, "/notifications").getBody()).containsEntry("unreadCount", 1);
        assertThat(getAs(USER_B, "/notifications").getBody()).containsEntry("unreadCount", 2);

        // The read flag in the list is per caller.
        assertThat(broadcastItemFor(USER_A).get("read")).isEqualTo(true);
        assertThat(broadcastItemFor(USER_B).get("read")).isEqualTo(false);
    }

    @Test
    void unreadOnly_filtersOutReadNotifications() {
        UUID id = firstNotificationIdFor(USER_A);
        postAs(USER_A, "/notifications/" + id + "/read");

        ResponseEntity<Map> response = getAs(USER_A, "/notifications?unreadOnly=true");

        assertThat(response.getBody())
                .containsEntry("total", 1)
                .containsEntry("unreadCount", 1);
    }

    @Test
    void markRead_isIdempotent() {
        UUID id = firstNotificationIdFor(USER_A);

        assertThat(postAs(USER_A, "/notifications/" + id + "/read").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(postAs(USER_A, "/notifications/" + id + "/read").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getAs(USER_A, "/notifications").getBody()).containsEntry("unreadCount", 1);
    }

    @Test
    void markRead_returns404ForUnknownId() {
        ResponseEntity<Void> response = postAs(USER_A, "/notifications/" + UUID.randomUUID() + "/read");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void markRead_returns404ForAnotherUsersPersonalNotification() {
        UUID bPersonalId = personalNotificationIdFor(USER_B);

        ResponseEntity<Void> response = postAs(USER_A, "/notifications/" + bPersonalId + "/read");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getAs(USER_B, "/notifications").getBody()).containsEntry("unreadCount", 2);
    }

    @Test
    void markAllReadForA_doesNotTouchB() {
        assertThat(postAs(USER_A, "/notifications/read-all").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getAs(USER_A, "/notifications?unreadOnly=true").getBody())
                .containsEntry("total", 0);
        assertThat(getAs(USER_B, "/notifications?unreadOnly=true").getBody())
                .containsEntry("total", 2);
    }

    @Test
    void actorDoesNotSeeOwnBroadcast() {
        notificationService.record(event("incident.created")
                .title("DB latency").severity("SEV2").actorId(USER_A).build());

        assertThat(getAs(USER_A, "/notifications").getBody()).containsEntry("total", 2);
        assertThat(getAs(USER_B, "/notifications").getBody()).containsEntry("total", 3);
    }

    @Test
    void commentFanOut_notifiesAssigneesExceptAuthor() {
        notificationService.record(event("incident.comment.added")
                .commentId(UUID.randomUUID())
                .content("Rolled back v2.4.1")
                .assignedUserIds(List.of(USER_A, USER_B))
                .actorId(USER_A)
                .build());

        assertThat(getAs(USER_A, "/notifications").getBody()).containsEntry("total", 2);
        assertThat(getAs(USER_B, "/notifications").getBody()).containsEntry("total", 3);
    }

    private Map<?, ?> broadcastItemFor(UUID user) {
        List<?> items = (List<?>) getAs(user, "/notifications").getBody().get("items");
        return items.stream()
                .map(item -> (Map<?, ?>) item)
                .filter(item -> item.get("recipientId") == null)
                .findFirst()
                .orElseThrow();
    }

    private UUID personalNotificationIdFor(UUID user) {
        List<?> items = (List<?>) getAs(user, "/notifications").getBody().get("items");
        return items.stream()
                .map(item -> (Map<?, ?>) item)
                .filter(item -> user.toString().equals(item.get("recipientId")))
                .map(item -> UUID.fromString((String) item.get("id")))
                .findFirst()
                .orElseThrow();
    }

    private UUID firstNotificationIdFor(UUID user) {
        List<?> items = (List<?>) getAs(user, "/notifications").getBody().get("items");
        return UUID.fromString((String) ((Map<?, ?>) items.get(0)).get("id"));
    }

    private ResponseEntity<Map> getAs(UUID user, String path) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(userHeaders(user)), Map.class);
    }

    private ResponseEntity<Void> postAs(UUID user, String path) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(userHeaders(user)), Void.class);
    }

    private static HttpHeaders userHeaders(UUID user) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", user.toString());
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
