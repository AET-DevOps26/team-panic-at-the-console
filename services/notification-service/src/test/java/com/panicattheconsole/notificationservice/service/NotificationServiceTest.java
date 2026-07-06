package com.panicattheconsole.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.domain.NotificationType;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    private IncidentEvent event(String subject, UUID assignedUserId, String newSeverity) {
        return new IncidentEvent(
                UUID.randomUUID(),
                subject,
                Instant.parse("2026-06-12T12:00:00Z"),
                assignedUserId,
                newSeverity,
                null);
    }

    private Notification recordAndCapture(IncidentEvent event) {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        Optional<Notification> result = service.record(event);
        assertThat(result).isPresent();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void incidentCreated_isBroadcast() {
        Notification n = recordAndCapture(event("incident.created", null, null));
        assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_CREATED);
        assertThat(n.getRecipientId()).isNull();
        assertThat(n.isRead()).isFalse();
    }

    @Test
    void severityEscalated_includesLevelInMessage() {
        Notification n = recordAndCapture(event("incident.severity.escalated", null, "SEV1"));
        assertThat(n.getType()).isEqualTo(NotificationType.SEVERITY_ESCALATED);
        assertThat(n.getMessage()).contains("SEV1");
        assertThat(n.getRecipientId()).isNull();
    }

    @Test
    void incidentResolved_isBroadcast() {
        Notification n = recordAndCapture(event("incident.resolved", null, null));
        assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_RESOLVED);
        assertThat(n.getRecipientId()).isNull();
    }

    @Test
    void commentAdded_isBroadcast() {
        Notification n = recordAndCapture(event("incident.comment.added", null, null));
        assertThat(n.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(n.getRecipientId()).isNull();
    }

    @Test
    void incidentAssigned_targetsTheAssignedUser() {
        UUID userId = UUID.randomUUID();
        Notification n = recordAndCapture(event("incident.assigned", userId, null));
        assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_ASSIGNED);
        assertThat(n.getRecipientId()).isEqualTo(userId);
    }

    @Test
    void unknownSubject_isNotStored() {
        Optional<Notification> result = service.record(event("incident.updated", null, null));
        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }
}
