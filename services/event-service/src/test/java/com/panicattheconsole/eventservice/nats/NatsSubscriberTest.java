package com.panicattheconsole.eventservice.nats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.eventservice.messaging.EventEnvelope;
import com.panicattheconsole.eventservice.service.TimelineService;

import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
class NatsSubscriberTest {

    @Mock
    private Connection natsConnection;

    @Mock
    private TimelineService timelineService;

    @Mock
    private Message message;

    private ObjectMapper objectMapper;
    private NatsSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        subscriber = new NatsSubscriber(
                natsConnection,
                objectMapper,
                timelineService);
    }

    @Test
    void incidentCreated_isStored() throws Exception {

        UUID incidentId = UUID.randomUUID();

        String payload = """
                {
                  "incidentId":"%s",
                  "timestamp":"2026-06-12T12:00:00Z"
                }
                """.formatted(incidentId);

        invokeHandleMessage(
                "incident.created",
                payload);

        EventEnvelope expectedEvent = new EventEnvelope(
                incidentId,
                "incident.created",
                Instant.parse("2026-06-12T12:00:00Z"),
                objectMapper.readTree(payload));

        verify(timelineService)
                .append(eq(expectedEvent));
    }

    @Test
    void incidentAssigned_isStored() throws Exception {

        UUID incidentId = UUID.randomUUID();

        String payload = """
                {
                  "incidentId":"%s",
                  "userId":"123",
                  "timestamp":"2026-06-12T12:00:00Z"
                }
                """.formatted(incidentId);

        invokeHandleMessage(
                "incident.assigned",
                payload);

        EventEnvelope expectedEvent = new EventEnvelope(
                incidentId,
                "incident.assigned",
                Instant.parse("2026-06-12T12:00:00Z"),
                objectMapper.readTree(payload));

        verify(timelineService)
                .append(eq(expectedEvent));
    }

    @Test
    void missingIncidentId_isIgnored() throws Exception {

        String payload = """
                {
                  "timestamp":"2026-06-12T12:00:00Z"
                }
                """;

        invokeHandleMessage(
                "incident.created",
                payload);

        verify(timelineService, never())
                .append(any());
    }

    @Test
    void missingTimestamp_isIgnored() throws Exception {

        UUID incidentId = UUID.randomUUID();

        String payload = """
                {
                  "incidentId":"%s"
                }
                """.formatted(incidentId);

        invokeHandleMessage(
                "incident.created",
                payload);

        verify(timelineService, never())
                .append(any());
    }

    @Test
    void invalidIncidentId_isIgnored() throws Exception {

        String payload = """
                {
                  "incidentId":"not-a-uuid",
                  "timestamp":"2026-06-12T12:00:00Z"
                }
                """;

        invokeHandleMessage(
                "incident.created",
                payload);

        verify(timelineService, never())
                .append(any());
    }

    private void invokeHandleMessage(
            String subject,
            String payload) throws Exception {

        org.mockito.Mockito.when(message.getSubject())
                .thenReturn(subject);

        org.mockito.Mockito.when(message.getData())
                .thenReturn(
                        payload.getBytes(StandardCharsets.UTF_8));

        Method method = NatsSubscriber.class
                .getDeclaredMethod(
                        "handleMessage",
                        Message.class);

        method.setAccessible(true);

        method.invoke(
                subscriber,
                message);
    }
}
