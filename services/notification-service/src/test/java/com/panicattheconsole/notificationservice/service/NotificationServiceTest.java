package com.panicattheconsole.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.domain.NotificationType;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.repository.NotificationReadRepository;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-06-12T12:00:00Z");

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationReadRepository readRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, readRepository);
    }

    private static IncidentEvent.Builder event(String subject) {
        return new IncidentEvent.Builder(subject)
                .incidentId(UUID.randomUUID())
                .timestamp(TIMESTAMP);
    }

    private List<Notification> recordAndCapture(IncidentEvent event) {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        List<Notification> saved = service.record(event);
        assertThat(saved).isNotEmpty();
        return saved;
    }

    @Test
    void incidentCreated_isBroadcastWithTitleAndSeverity() {
        List<Notification> saved = recordAndCapture(
                event("incident.created").title("Checkout down").severity("SEV1").build());

        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_CREATED);
        assertThat(n.getRecipientId()).isNull();
        assertThat(n.getMessage()).isEqualTo("New incident: Checkout down (SEV1)");
    }

    @Test
    void incidentCreated_withoutTitle_fallsBackToGenericMessage() {
        List<Notification> saved = recordAndCapture(event("incident.created").build());
        assertThat(saved.get(0).getMessage()).isEqualTo("A new incident was opened.");
    }

    @Test
    void incidentCreated_storesActorForReadSideSuppression() {
        UUID actor = UUID.randomUUID();
        List<Notification> saved = recordAndCapture(
                event("incident.created").actorId(actor).build());
        assertThat(saved.get(0).getActorId()).isEqualTo(actor);
    }

    @Test
    void severityEscalated_fansOutToAssigneesExceptActor() {
        UUID assignee = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        List<Notification> saved = recordAndCapture(
                event("incident.severity.escalated")
                        .newSeverity("SEV1")
                        .assignedUserIds(List.of(assignee, actor))
                        .actorId(actor)
                        .build());

        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getType()).isEqualTo(NotificationType.SEVERITY_ESCALATED);
        assertThat(n.getRecipientId()).isEqualTo(assignee);
        assertThat(n.getMessage()).contains("SEV1");
    }

    @Test
    void severityEscalated_withoutAssignees_storesNothing() {
        List<Notification> saved = service.record(
                event("incident.severity.escalated").newSeverity("SEV1").build());
        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void statusChanged_toResolved_usesResolvedType() {
        UUID assignee = UUID.randomUUID();
        List<Notification> saved = recordAndCapture(
                event("incident.status.changed")
                        .newStatus("resolved")
                        .assignedUserIds(List.of(assignee))
                        .build());

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getType()).isEqualTo(NotificationType.INCIDENT_RESOLVED);
        assertThat(saved.get(0).getRecipientId()).isEqualTo(assignee);
    }

    @Test
    void statusChanged_toInvestigating_usesStatusChangedType() {
        UUID assignee = UUID.randomUUID();
        List<Notification> saved = recordAndCapture(
                event("incident.status.changed")
                        .newStatus("investigating")
                        .assignedUserIds(List.of(assignee))
                        .build());

        assertThat(saved.get(0).getType()).isEqualTo(NotificationType.STATUS_CHANGED);
        assertThat(saved.get(0).getMessage()).contains("investigating");
    }

    @Test
    void commentAdded_fansOutWithTruncatedContent() {
        UUID assignee = UUID.randomUUID();
        String longContent = "x".repeat(200);
        List<Notification> saved = recordAndCapture(
                event("incident.comment.added")
                        .content(longContent)
                        .assignedUserIds(List.of(assignee))
                        .build());

        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(n.getRecipientId()).isEqualTo(assignee);
        assertThat(n.getMessage()).startsWith("New comment: \"" + "x".repeat(120) + "...");
    }

    @Test
    void commentAdded_authorIsNotNotified() {
        UUID author = UUID.randomUUID();
        List<Notification> saved = service.record(
                event("incident.comment.added")
                        .content("done")
                        .assignedUserIds(List.of(author))
                        .actorId(author)
                        .build());
        assertThat(saved).isEmpty();
    }

    @Test
    void incidentAssigned_targetsTheAssignedUser() {
        UUID userId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        List<Notification> saved = recordAndCapture(
                event("incident.assigned").assignedUserId(userId).actorId(actor).build());

        assertThat(saved).hasSize(1);
        Notification n = saved.get(0);
        assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_ASSIGNED);
        assertThat(n.getRecipientId()).isEqualTo(userId);
        assertThat(n.getActorId()).isEqualTo(actor);
    }

    @Test
    void incidentAssigned_selfAssignmentIsSuppressed() {
        UUID userId = UUID.randomUUID();
        List<Notification> saved = service.record(
                event("incident.assigned").assignedUserId(userId).actorId(userId).build());
        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void unknownSubject_isNotStored() {
        List<Notification> saved = service.record(event("incident.updated").build());
        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void markRead_rejectsForeignPersonalNotification() {
        UUID owner = UUID.randomUUID();
        Notification personal = new Notification(
                UUID.randomUUID(), NotificationType.INCIDENT_ASSIGNED, owner,
                "You were assigned to an incident.", TIMESTAMP, null);
        when(repository.findById(personal.getId())).thenReturn(Optional.of(personal));

        assertThat(service.markRead(personal.getId(), UUID.randomUUID())).isFalse();
        verify(readRepository, never()).insertReadMark(any(), any());
    }

    @Test
    void markRead_insertsMarkForVisibleNotification() {
        UUID reader = UUID.randomUUID();
        Notification broadcast = new Notification(
                UUID.randomUUID(), NotificationType.INCIDENT_CREATED, null,
                "New incident: Checkout down (SEV1)", TIMESTAMP, null);
        when(repository.findById(broadcast.getId())).thenReturn(Optional.of(broadcast));

        assertThat(service.markRead(broadcast.getId(), reader)).isTrue();
        verify(readRepository).insertReadMark(broadcast.getId(), reader);
    }

    @Test
    void markRead_rejectsOwnActionBroadcast() {
        UUID actor = UUID.randomUUID();
        Notification broadcast = new Notification(
                UUID.randomUUID(), NotificationType.INCIDENT_CREATED, null,
                "New incident: Checkout down (SEV1)", TIMESTAMP, actor);
        when(repository.findById(broadcast.getId())).thenReturn(Optional.of(broadcast));

        assertThat(service.markRead(broadcast.getId(), actor)).isFalse();
        verify(readRepository, never()).insertReadMark(any(), any());
    }
}
