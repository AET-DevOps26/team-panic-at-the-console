package com.panicattheconsole.incidentservice.incident;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.panicattheconsole.incidentservice.nats.NatsEventPublisher;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NatsEventPublisher natsEventPublisher;

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
    void requestRegeneration_publishesEvent_whenIncidentExists() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.requestRegeneration(incidentId);

        verify(natsEventPublisher).publishIncidentRegenRequested(incidentId);
    }

    @Test
    void requestRegeneration_throwsNotFound_whenIncidentMissing() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.requestRegeneration(incidentId))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Incident not found");

        verify(natsEventPublisher, never()).publishIncidentRegenRequested(any());
    }

    @Test
    void validatePostmortermAllowed_allowsResolvedIncident() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        incidentService.validatePostmortermAllowed(incidentId);
    }

    @Test
    void validatePostmortermAllowed_throwsForNonResolvedIncident() {
        incident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.validatePostmortermAllowed(incidentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot regenerate postmortem");
    }

    @Test
    void updatePostmortem_publishesUpdate_whenIncidentResolved() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);

        incidentService.updatePostmortem(incidentId, "Final postmortem content");

        assertThat(incident.getPostmortem()).isEqualTo("Final postmortem content");
        verify(natsEventPublisher).publishIncidentUpdated(incidentId);
    }

    @Test
    void updatePostmortem_throwsForNonResolvedIncident() {
        incident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.updatePostmortem(incidentId, "Content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot set postmortem for non-resolved incident");

        verify(natsEventPublisher, never()).publishIncidentUpdated(any());
    }
}
