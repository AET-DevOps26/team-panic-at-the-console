package com.panicattheconsole.incidentservice.nats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.panicattheconsole.incidentservice.rule.RuleEvaluator;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panicattheconsole.incidentservice.incident.IncidentService;
import com.panicattheconsole.incidentservice.incident.Severity;

import io.nats.client.Connection;
import io.nats.client.Message;

@ExtendWith(MockitoExtension.class)
class NatsSubscriberTest {

        @Mock
        private Connection natsConnection;

        @Mock
        private IncidentService incidentService;

        @Mock
        private RuleEvaluator ruleEvaluator;

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
                                incidentService,
                                ruleEvaluator);
        }

        @Test
        void createRequested_createsIncident() throws Exception {
                UUID sourceId = UUID.randomUUID();

                String payload = """
                                {
                                  "sourceId":"%s",
                                  "severity":"SEV3"
                                }
                                """.formatted(sourceId);

                invokeHandleMessage(
                                "incident.create.requested",
                                payload);

                verify(incidentService).createIncident(
                                any(UUID.class),
                                eq(Severity.SEV3),
                                eq("Created from external event"),
                                eq(sourceId));
        }

        @Test
        void externalEventReceived_delegatesRawMessageToEvaluator() throws Exception {
                UUID externalEventId = UUID.randomUUID();

                String payload = """
                                {
                                  "sourceId":"%s",
                                  "source":"github",
                                  "eventType":"ci_failure",
                                  "timestamp":"2026-07-15T10:00:00Z",
                                  "rawPayload":{
                                    "repository":"demo/repo",
                                    "workflow":"build"
                                  }
                                }
                                """.formatted(externalEventId);

                invokeHandleMessage("external.event.received", payload);

                verify(ruleEvaluator).evaluate(payload);
        }

        @Test
        void createRequested_invalidUuid_isIgnored() throws Exception {
                String payload = """
                                {
                                  "sourceId":"not-a-uuid",
                                  "severity":"SEV3"
                                }
                                """;

                invokeHandleMessage(
                                "incident.create.requested",
                                payload);

                verify(incidentService, never())
                                .createIncident(any(), any(), any(), any());
        }

        @Test
        void createRequested_unknownSeverity_isIgnored() throws Exception {
                UUID sourceId = UUID.randomUUID();

                String payload = """
                                {
                                  "sourceId":"%s",
                                  "severity":"SEV999"
                                }
                                """.formatted(sourceId);

                invokeHandleMessage(
                                "incident.create.requested",
                                payload);

                verify(incidentService, never())
                                .createIncident(any(), any(), any(), any());
        }

        @Test
        void createRequested_missingFields_isIgnored() throws Exception {
                String payload = """
                                {
                                  "sourceId": null,
                                  "severity":"SEV3"
                                }
                                """;

                invokeHandleMessage(
                                "incident.create.requested",
                                payload);

                verify(incidentService, never())
                                .createIncident(any(), any(), any(), any());
        }

        @Test
        void severityEscalationRequested_escalatesSeverity() throws Exception {
                UUID incidentId = UUID.randomUUID();

                String payload = """
                                {
                                  "incidentId":"%s",
                                  "requestedSeverity":"SEV1"
                                }
                                """.formatted(incidentId);

                invokeHandleMessage(
                                "incident.severity.escalate.requested",
                                payload);

                verify(incidentService)
                                .escalateSeverity(
                                                incidentId,
                                                Severity.SEV1);
        }

        @Test
        void severityEscalationRequested_invalidUuid_isIgnored() throws Exception {
                String payload = """
                                {
                                  "incidentId":"invalid",
                                  "requestedSeverity":"SEV1"
                                }
                                """;

                invokeHandleMessage(
                                "incident.severity.escalate.requested",
                                payload);

                verify(incidentService, never())
                                .escalateSeverity(any(), any());
        }

        @Test
        void severityEscalationRequested_unknownSeverity_isIgnored() throws Exception {
                UUID incidentId = UUID.randomUUID();

                String payload = """
                                {
                                  "incidentId":"%s",
                                  "requestedSeverity":"INVALID"
                                }
                                """.formatted(incidentId);

                invokeHandleMessage(
                                "incident.severity.escalate.requested",
                                payload);

                verify(incidentService, never())
                                .escalateSeverity(any(), any());
        }

        @Test
        void unknownSubject_isIgnored() throws Exception {
                invokeHandleMessage(
                                "some.random.subject",
                                "{}");

                verify(incidentService, never())
                                .createIncident(any(), any(), any(), any());

                verify(incidentService, never())
                                .escalateSeverity(any(), any());
        }

        private void invokeHandleMessage(String subject, String payload)
                        throws Exception {

                org.mockito.Mockito.when(message.getSubject())
                                .thenReturn(subject);

                org.mockito.Mockito.when(message.getData())
                                .thenReturn(payload.getBytes(StandardCharsets.UTF_8));

                Method method = NatsSubscriber.class
                                .getDeclaredMethod("handleMessage", Message.class);

                method.setAccessible(true);
                method.invoke(subscriber, message);
        }
}
