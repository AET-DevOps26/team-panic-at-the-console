package com.panicattheconsole.incidentservice.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
        verify(applicationEventPublisher).publishEvent(any(IncidentNatsEvent.class));
    }

    @Test
    void updateIncidentStatus_allowsInvestigatingToResolved() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updateIncidentStatus(incidentId, IncidentStatus.RESOLVED);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        ArgumentCaptor<IncidentNatsEvent> eventCaptor = ArgumentCaptor.forClass(IncidentNatsEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<String> subjects = eventCaptor.getAllValues().stream()
                .map(IncidentNatsEvent::getSubject)
                .toList();

        assertThat(subjects).containsExactlyInAnyOrder("incident.updated", "incident.resolved");
    }

    @Test
    void updateIncidentStatus_rejectsResolvedToInvestigating() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updateIncidentStatus(incidentId, IncidentStatus.INVESTIGATING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateIncidentStatus_rejectsInvestigatingToOpen() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updateIncidentStatus(incidentId, IncidentStatus.OPEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");

        verify(applicationEventPublisher, never()).publishEvent(any());
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
}
