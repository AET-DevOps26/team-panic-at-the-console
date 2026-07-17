package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.openapitools.model.RegenAccepted;

import com.panicattheconsole.incidentservice.nats.IncidentNatsEvent;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private IncidentService incidentService;

    private UUID incidentId;
    private Incident incident;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        incident = new Incident(incidentId, IncidentStatus.OPEN, Severity.SEV2, "Test incident");
    }

    @Test
    void requestRegeneration_publishesEventWithTask_whenIncidentExists() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.requestRegeneration(incidentId, RegenAccepted.TaskEnum.SUMMARY);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("incident.regen.requested");
        assertThat(captor.getValue().getPayload())
                .containsEntry("task", RegenAccepted.TaskEnum.SUMMARY.getValue());
    }

    @Test
    void requestRegeneration_throwsNotFound_whenIncidentMissing() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.requestRegeneration(
                incidentId, RegenAccepted.TaskEnum.SUMMARY))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Incident not found");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteIncident_removesCommentsAndIncidentAndPublishesDeletedEvent() {
        UUID actorId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        incident.setAssignedUsers(Set.of(assigneeId));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.deleteIncident(incidentId, actorId);

        verify(commentRepository).deleteByIncident_Id(incidentId);
        verify(incidentRepository).delete(incident);
        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("incident.deleted");
        assertThat(captor.getValue().getPayload())
                .containsEntry("incidentId", incidentId.toString())
                .containsEntry("title", "Test incident")
                .containsEntry("actorId", actorId.toString())
                .containsEntry("assignedUserIds", List.of(assigneeId.toString()));
    }

    @Test
    void deleteIncident_throwsNotFound_whenIncidentMissing() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.deleteIncident(incidentId, null))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Incident not found");

        verify(incidentRepository, never()).delete(any(Incident.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void validatePostmortemAllowed_allowsResolvedIncident() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.validatePostmortemAllowed(incidentId);
    }

    @Test
    void validatePostmortemAllowed_throwsForNonResolvedIncident() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.validatePostmortemAllowed(incidentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot regenerate postmortem");
    }

    @Test
    void requestPostmortemRegeneration_publishesPostmortemTask_whenIncidentResolved() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.requestPostmortemRegeneration(incidentId);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getPayload())
                .containsEntry("task", RegenAccepted.TaskEnum.POSTMORTEM.getValue());
    }

    @Test
    void requestPostmortemRegeneration_throwsForNonResolvedIncident() {
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.requestPostmortemRegeneration(incidentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot regenerate postmortem");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateIncidentStatus_allowsOpenToInvestigating() {
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateIncidentStatus(incidentId, IncidentStatus.INVESTIGATING);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);

        ArgumentCaptor<IncidentNatsEvent> eventCaptor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<String> subjects = eventCaptor.getAllValues().stream()
                .map(IncidentNatsEvent::getSubject)
                .toList();
        assertThat(subjects).containsExactlyInAnyOrder("incident.updated", "incident.status.changed");

        IncidentNatsEvent statusEvent = eventCaptor.getAllValues().stream()
                .filter(e -> e.getSubject().equals("incident.status.changed"))
                .findFirst()
                .orElseThrow();
        assertThat(statusEvent.getPayload())
                .containsEntry("oldStatus", "open")
                .containsEntry("newStatus", "investigating");
    }

    @Test
    void updateIncidentStatus_allowsInvestigatingToResolved() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateIncidentStatus(incidentId, IncidentStatus.RESOLVED);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        ArgumentCaptor<IncidentNatsEvent> eventCaptor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<String> subjects = eventCaptor.getAllValues().stream()
                .map(IncidentNatsEvent::getSubject)
                .toList();

        assertThat(subjects).containsExactlyInAnyOrder(
                "incident.updated", "incident.status.changed", "incident.resolved");
    }

    @Test
    void updateIncidentStatus_reopensResolvedIncidentAndClearsResolvedAt() {
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(java.time.Instant.now());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateIncidentStatus(incidentId, IncidentStatus.INVESTIGATING);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(incident.getResolvedAt()).isNull();

        ArgumentCaptor<IncidentNatsEvent> eventCaptor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        IncidentNatsEvent statusEvent = eventCaptor.getAllValues().stream()
                .filter(e -> e.getSubject().equals("incident.status.changed"))
                .findFirst()
                .orElseThrow();
        assertThat(statusEvent.getPayload())
                .containsEntry("oldStatus", "resolved")
                .containsEntry("newStatus", "investigating");
    }

    @Test
    void updateIncidentStatus_allowsInvestigatingToOpen() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateIncidentStatus(incidentId, IncidentStatus.OPEN);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        verify(applicationEventPublisher, times(2)).publishEvent(any(IncidentNatsEvent.class));
    }

    @Test
    void updateIncidentStatus_rejectsSameStatusTransition() {
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updateIncidentStatus(incidentId, IncidentStatus.OPEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createIncident_publishesCreatedEventWithTitleAndSeverity() {
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        incidentService.createIncident(incidentId, Severity.SEV2, "Test incident", null);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("incident.created");
        assertThat(captor.getValue().getPayload())
                .containsEntry("title", "Test incident")
                .containsEntry("severity", "SEV2");
    }

    @Test
    void escalateSeverity_publishesOldAndNewSeverity() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.escalateSeverity(incidentId, Severity.SEV1);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("incident.severity.escalated");
        assertThat(captor.getValue().getPayload())
                .containsEntry("oldSeverity", "SEV2")
                .containsEntry("newSeverity", "SEV1")
                .containsEntry("assignedUserIds", List.of())
                .doesNotContainKey("actorId");
    }

    @Test
    void escalateSeverity_carriesAssigneesAndActor() {
        UUID assignee = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        incident.setAssignedUsers(new java.util.HashSet<>(Set.of(assignee)));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.escalateSeverity(incidentId, Severity.SEV1, actor);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getPayload())
                .containsEntry("assignedUserIds", List.of(assignee.toString()))
                .containsEntry("actorId", actor.toString());
    }

    @Test
    void addComment_publishesCommentContent() {
        UUID commentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        incident.setAssignedUsers(new java.util.HashSet<>(Set.of(assignee)));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        incidentService.addComment(incidentId, commentId, authorId, "Rolled back v2.4.1");

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("incident.comment.added");
        assertThat(captor.getValue().getPayload())
                .containsEntry("commentId", commentId.toString())
                .containsEntry("content", "Rolled back v2.4.1")
                .containsEntry("assignedUserIds", List.of(assignee.toString()))
                .containsEntry("actorId", authorId.toString());
    }

    @Test
    void updatePostmortem_publishesUpdate_whenIncidentResolved() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updatePostmortem(incidentId, "Final postmortem content");

        assertThat(incident.getPostmortem()).isEqualTo("Final postmortem content");
        verify(applicationEventPublisher).publishEvent(any(IncidentNatsEvent.class));
    }

    @Test
    void updatePostmortem_throwsForNonResolvedIncident() {
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updatePostmortem(incidentId, "Content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot set postmortem for non-resolved incident");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void listIncidents_filtersByStatusAndSeverity() {
        when(incidentRepository.findByStatusAndSeverity(eq(IncidentStatus.OPEN), eq(Severity.SEV2),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(incident)));

        List<Incident> result = incidentService.listIncidents(
                IncidentStatus.OPEN,
                Severity.SEV2,
                0,
                10);

        assertThat(result).containsExactly(incident);
    }

    @Test
    void countIncidents_returnsMatchingCount() {
        when(incidentRepository.countByStatusAndSeverity(IncidentStatus.OPEN, Severity.SEV2)).thenReturn(5L);

        long count = incidentService.countIncidents(IncidentStatus.OPEN, Severity.SEV2);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void updateDescription_setsDescriptionAndPublishesEvent() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        Incident saved = incidentService.updateDescription(incidentId, "Checkout errors after deploy");

        assertThat(saved.getDescription()).isEqualTo("Checkout errors after deploy");
        verify(applicationEventPublisher).publishEvent(any(IncidentNatsEvent.class));
    }

    @Test
    void updateDescription_blankInputClearsDescription() {
        incident.setDescription("old description");
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        Incident saved = incidentService.updateDescription(incidentId, "   ");

        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void updateAssignedUsers_replacesExistingUsers() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);
        UUID newUser = UUID.randomUUID();

        Incident saved = incidentService.updateAssignedUsers(incidentId, Set.of(newUser), null);

        assertThat(saved.getAssignedUsers()).containsExactly(newUser);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(IncidentNatsEvent::getSubject)
                .containsExactly("incident.updated", "incident.assigned");
        assertThat(captor.getAllValues().get(1).getPayload())
                .containsEntry("userId", newUser.toString())
                .doesNotContainKey("actorId");
    }

    @Test
    void updateAssignedUsers_publishesAssignedOnlyForNewlyAddedUsers() {
        UUID existingUser = UUID.randomUUID();
        UUID addedUser = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        incident.setAssignedUsers(new java.util.HashSet<>(Set.of(existingUser)));
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateAssignedUsers(incidentId, Set.of(existingUser, addedUser), actor);

        ArgumentCaptor<IncidentNatsEvent> captor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());
        List<IncidentNatsEvent> assignedEvents = captor.getAllValues().stream()
                .filter(e -> e.getSubject().equals("incident.assigned"))
                .toList();
        assertThat(assignedEvents).hasSize(1);
        assertThat(assignedEvents.get(0).getPayload())
                .containsEntry("userId", addedUser.toString())
                .containsEntry("actorId", actor.toString());
    }

    @Test
    void listComments_returnsCommentsFromRepository() {
        Comment comment = new Comment(UUID.randomUUID(), UUID.randomUUID(), "Note");
        comment.setIncident(incident);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(commentRepository.findByIncident_IdOrderByCreatedAtAsc(eq(incidentId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

        List<Comment> comments = incidentService.listComments(incidentId, 0, 10);

        assertThat(comments).containsExactly(comment);
    }

    @Test
    void countComments_returnsCorrectTotal() {
        when(commentRepository.countByIncident_Id(incidentId)).thenReturn(7L);

        long total = incidentService.countComments(incidentId);

        assertThat(total).isEqualTo(7L);
    }
}
