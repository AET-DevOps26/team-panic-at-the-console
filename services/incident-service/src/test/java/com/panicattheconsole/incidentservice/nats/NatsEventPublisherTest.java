package com.panicattheconsole.incidentservice.nats;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;

@ExtendWith(MockitoExtension.class)
class NatsEventPublisherTest {

    @Mock
    private Connection natsConnection;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NatsEventPublisher publisher;

    private UUID incidentId;

    @BeforeEach
    void setUp() throws Exception {
        incidentId = UUID.randomUUID();
    }

    @Test
    void publishIncidentCreated_sendsExpectedPayload() throws Exception {
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        String expectedJson = "{\"incidentId\":\"" + incidentId + "\",\"timestamp\":\"2026-01-01T00:00:00Z\"}";

        org.mockito.Mockito.when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenReturn(expectedJson);

        publisher.publishIncidentCreated(incidentId);

        verify(natsConnection).publish(eq("incident.created"), payloadCaptor.capture());
        String payload = new String(payloadCaptor.getValue());
        assertThat(payload).isEqualTo(expectedJson);
    }

    @Test
    void publishIncidentSeverityEscalated_includesNewSeverity() throws Exception {
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        String expectedJson = "{\"incidentId\":\"" + incidentId + "\",\"timestamp\":\"2026-01-01T00:00:00Z\",\"newSeverity\":\"SEV1\"}";

        org.mockito.Mockito.when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenReturn(expectedJson);

        publisher.publishIncidentSeverityEscalated(incidentId, "SEV1");

        verify(natsConnection).publish(eq("incident.severity.escalated"), payloadCaptor.capture());
        String payload = new String(payloadCaptor.getValue());
        assertThat(payload).contains("\"newSeverity\":\"SEV1\"");
    }
}
