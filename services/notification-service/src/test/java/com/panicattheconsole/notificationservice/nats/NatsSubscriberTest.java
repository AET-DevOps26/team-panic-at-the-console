package com.panicattheconsole.notificationservice.nats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.service.NotificationService;

import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
class NatsSubscriberTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Message message;

    private ObjectMapper objectMapper;
    private NatsSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Message handling is exercised directly; no live NATS connection is opened.
        subscriber = new NatsSubscriber("nats://localhost:4222", false, objectMapper, notificationService);
    }

    @Test
    void incidentCreated_isRecorded() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.created", """
                {"incidentId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.incidentId()).isEqualTo(incidentId);
        assertThat(event.subject()).isEqualTo("incident.created");
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-06-12T12:00:00Z"));
        assertThat(event.assignedUserId()).isNull();
    }

    @Test
    void severityEscalated_carriesNewSeverity() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.severity.escalated", """
                {"incidentId":"%s","newSeverity":"SEV1","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.newSeverity()).isEqualTo("SEV1");
    }

    @Test
    void incidentAssigned_carriesUserId() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        invokeHandleMessage("incident.assigned", """
                {"incidentId":"%s","userId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId, userId));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.assignedUserId()).isEqualTo(userId);
    }

    @Test
    void incidentCreated_carriesTitleSeverityAndActor() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        invokeHandleMessage("incident.created", """
                {"incidentId":"%s","title":"Checkout down","severity":"SEV1",
                 "actorId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId, actorId));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.title()).isEqualTo("Checkout down");
        assertThat(event.severity()).isEqualTo("SEV1");
        assertThat(event.actorId()).isEqualTo(actorId);
    }

    @Test
    void commentAdded_carriesContentAndAssignees() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        invokeHandleMessage("incident.comment.added", """
                {"incidentId":"%s","commentId":"%s","content":"Rolled back v2.4.1",
                 "assignedUserIds":["%s"],"timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId, UUID.randomUUID(), assignee));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.content()).isEqualTo("Rolled back v2.4.1");
        assertThat(event.assignedUserIds()).containsExactly(assignee);
    }

    @Test
    void statusChanged_carriesNewStatus() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.status.changed", """
                {"incidentId":"%s","oldStatus":"open","newStatus":"investigating",
                 "timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        IncidentEvent event = captureRecordedEvent();
        assertThat(event.newStatus()).isEqualTo("investigating");
    }

    @Test
    void statusChanged_withoutNewStatus_isIgnored() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.status.changed", """
                {"incidentId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        verify(notificationService, never()).record(any());
    }

    @Test
    void severityEscalated_withoutNewSeverity_isIgnored() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.severity.escalated", """
                {"incidentId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        verify(notificationService, never()).record(any());
    }

    @Test
    void incidentAssigned_withoutUserId_isIgnored() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.assigned", """
                {"incidentId":"%s","timestamp":"2026-06-12T12:00:00Z"}
                """.formatted(incidentId));

        verify(notificationService, never()).record(any());
    }

    @Test
    void missingIncidentId_isIgnored() throws Exception {
        invokeHandleMessage("incident.created", """
                {"timestamp":"2026-06-12T12:00:00Z"}
                """);

        verify(notificationService, never()).record(any());
    }

    @Test
    void missingTimestamp_isIgnored() throws Exception {
        UUID incidentId = UUID.randomUUID();
        invokeHandleMessage("incident.created", """
                {"incidentId":"%s"}
                """.formatted(incidentId));

        verify(notificationService, never()).record(any());
    }

    @Test
    void invalidIncidentId_isIgnored() throws Exception {
        invokeHandleMessage("incident.created", """
                {"incidentId":"not-a-uuid","timestamp":"2026-06-12T12:00:00Z"}
                """);

        verify(notificationService, never()).record(any());
    }

    private IncidentEvent captureRecordedEvent() {
        ArgumentCaptor<IncidentEvent> captor = ArgumentCaptor.forClass(IncidentEvent.class);
        verify(notificationService).record(captor.capture());
        return captor.getValue();
    }

    private void invokeHandleMessage(String subject, String payload) {
        org.mockito.Mockito.when(message.getSubject()).thenReturn(subject);
        org.mockito.Mockito.when(message.getData())
                .thenReturn(payload.getBytes(StandardCharsets.UTF_8));

        subscriber.handleMessage(message);
    }
}
