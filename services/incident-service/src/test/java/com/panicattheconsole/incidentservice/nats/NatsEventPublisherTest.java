package com.panicattheconsole.incidentservice.nats;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;

@ExtendWith(MockitoExtension.class)
class NatsEventPublisherTest {

    @Mock
    private Connection natsConnection;

    private ObjectMapper objectMapper;

    private NatsEventPublisher publisher;

    private UUID incidentId;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        objectMapper = new ObjectMapper();
        publisher = new NatsEventPublisher(natsConnection, objectMapper);
    }

    @Test
    void publishIncidentCreated_sendsExpectedPayload() throws Exception {
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);

        publisher.publishIncidentCreated(incidentId);

        verify(natsConnection).publish(eq("incident.created"), payloadCaptor.capture());
        byte[] payloadBytes = payloadCaptor.getValue();
        Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {});

        assertThat(payload).containsEntry("incidentId", incidentId.toString());
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    void publishIncidentSeverityEscalated_includesNewSeverity() throws Exception {
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);

        publisher.publishIncidentSeverityEscalated(incidentId, "SEV1");

        verify(natsConnection).publish(eq("incident.severity.escalated"), payloadCaptor.capture());
        Map<String, Object> payload = objectMapper.readValue(payloadCaptor.getValue(), new TypeReference<>() {});

        assertThat(payload).containsEntry("incidentId", incidentId.toString());
        assertThat(payload).containsEntry("newSeverity", "SEV1");
        assertThat(payload).containsKey("timestamp");
    }
}
