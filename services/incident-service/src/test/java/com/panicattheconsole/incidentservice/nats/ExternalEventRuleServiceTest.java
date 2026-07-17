package com.panicattheconsole.incidentservice.nats;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.incidentservice.incident.IncidentService;
import com.panicattheconsole.incidentservice.incident.Severity;

@ExtendWith(MockitoExtension.class)
class ExternalEventRuleServiceTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private ProcessedExternalEventRepository processedExternalEventRepository;

    private ExternalEventRuleService service;

    @BeforeEach
    void setUp() {
        service = new ExternalEventRuleService(incidentService, processedExternalEventRepository, new ObjectMapper());
    }

    @Test
    void duplicateExternalEvent_isIgnored() {
        UUID externalEventId = UUID.randomUUID();
        String payload = """
                {
                  "sourceId":"%s",
                  "eventType":"ci_failure"
                }
                """.formatted(externalEventId);

        when(processedExternalEventRepository.findByExternalEventId(externalEventId.toString()))
                .thenReturn(Optional.of(new ProcessedExternalEvent(externalEventId.toString())));

        boolean created = service.shouldCreateIncident("ci_failure", payload);

        assertFalse(created);
        verify(incidentService, never()).createIncident(any(), any(), any(), any(), any(), any());
    }

    @Test
    void matchingCiFailure_createsIncident() {
        UUID externalEventId = UUID.randomUUID();
        String payload = """
                {
                  "sourceId":"%s",
                  "eventType":"ci_failure"
                }
                """.formatted(externalEventId);

        when(processedExternalEventRepository.findByExternalEventId(externalEventId.toString()))
                .thenReturn(Optional.empty());
        when(processedExternalEventRepository.save(any(ProcessedExternalEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean created = service.shouldCreateIncident("ci_failure", payload);

        assertTrue(created);
        verify(incidentService).createIncident(any(UUID.class), eq(Severity.SEV2), any(String.class), any(String.class),
                eq(externalEventId), eq(null));
    }

    @Test
    void matchingFailureLikePayload_createsIncident() {
        UUID externalEventId = UUID.randomUUID();
        String payload = """
                {
                  "sourceId":"%s",
                  "eventType":"github.workflow_run",
                  "rawPayload":{
                    "conclusion":"failure"
                  }
                }
                """.formatted(externalEventId);

        when(processedExternalEventRepository.findByExternalEventId(externalEventId.toString()))
                .thenReturn(Optional.empty());
        when(processedExternalEventRepository.save(any(ProcessedExternalEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean created = service.shouldCreateIncident("github.workflow_run", payload);

        assertTrue(created);
        verify(incidentService).createIncident(any(UUID.class), eq(Severity.SEV2), any(String.class), any(String.class),
                eq(externalEventId), eq(null));
    }
}
