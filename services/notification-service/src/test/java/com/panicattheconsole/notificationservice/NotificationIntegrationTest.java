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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.nats.NatsSubscriber;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;
import com.panicattheconsole.notificationservice.service.NotificationService;

/**
 * Exercises the personal-vs-broadcast visibility model, unread counts, and the
 * mark-read flows against a real Postgres instance: logic that lives in custom
 * repository queries and cannot be covered by mock-based unit tests.
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

    @BeforeEach
    void seed() {
        notificationRepository.deleteAll();
        // Broadcast: visible to everyone.
        notificationService.record(new IncidentEvent(INCIDENT_ID, "incident.created", TS, null, null, null));
        // Personal notifications for two different users.
        notificationService.record(new IncidentEvent(INCIDENT_ID, "incident.assigned", TS, USER_A, null, null));
        notificationService.record(new IncidentEvent(INCIDENT_ID, "incident.assigned", TS, USER_B, null, null));
    }

    @Test
    void listForRecipient_returnsPersonalAndBroadcast_excludingOtherUsers() {
        ResponseEntity<Map> response = rest.getForEntity(url("/notifications?recipientId=" + USER_A), Map.class);

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
    void listWithoutRecipient_returnsEverything() {
        ResponseEntity<Map> response = rest.getForEntity(url("/notifications"), Map.class);

        assertThat(response.getBody()).containsEntry("total", 3);
    }

    @Test
    void unreadOnly_filtersOutReadNotifications() {
        UUID id = firstNotificationIdFor(USER_A);
        rest.postForEntity(url("/notifications/" + id + "/read"), null, Void.class);

        ResponseEntity<Map> response = rest.getForEntity(
                url("/notifications?recipientId=" + USER_A + "&unreadOnly=true"), Map.class);

        assertThat(response.getBody())
                .containsEntry("total", 1)
                .containsEntry("unreadCount", 1);
    }

    @Test
    void markRead_returns204_andPersistsReadState() {
        UUID id = firstNotificationIdFor(USER_A);

        ResponseEntity<Void> response = rest.postForEntity(url("/notifications/" + id + "/read"), null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(notificationRepository.findById(id))
                .get()
                .extracting(Notification::isRead)
                .isEqualTo(true);
    }

    @Test
    void markRead_returns404ForUnknownId() {
        ResponseEntity<Void> response = rest.postForEntity(
                url("/notifications/" + UUID.randomUUID() + "/read"), null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void markAllReadForRecipient_onlyAffectsThatRecipientsScope() {
        rest.postForEntity(url("/notifications/read-all?recipientId=" + USER_A), null, Void.class);

        // A's personal notification and the broadcast are now read.
        ResponseEntity<Map> forA = rest.getForEntity(
                url("/notifications?recipientId=" + USER_A + "&unreadOnly=true"), Map.class);
        assertThat(forA.getBody()).containsEntry("total", 0);

        // B's personal notification is untouched; the broadcast it shared is now read.
        ResponseEntity<Map> forB = rest.getForEntity(
                url("/notifications?recipientId=" + USER_B + "&unreadOnly=true"), Map.class);
        assertThat(forB.getBody()).containsEntry("total", 1);
    }

    private UUID firstNotificationIdFor(UUID recipient) {
        ResponseEntity<Map> response = rest.getForEntity(url("/notifications?recipientId=" + recipient), Map.class);
        List<?> items = (List<?>) response.getBody().get("items");
        return UUID.fromString((String) ((Map<?, ?>) items.get(0)).get("id"));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
