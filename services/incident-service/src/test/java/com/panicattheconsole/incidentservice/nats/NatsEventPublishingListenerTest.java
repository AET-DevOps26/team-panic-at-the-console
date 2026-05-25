package com.panicattheconsole.incidentservice.nats;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NatsEventPublishingListenerTest {

    @Test
    void handleIncidentNatsEvent_publishesAfterCommitEvent() {
        NatsEventPublisher publisher = Mockito.mock(NatsEventPublisher.class);
        NatsEventPublishingListener listener = new NatsEventPublishingListener(publisher);

        Map<String, Object> payload = new HashMap<>();
        payload.put("incidentId", "123");
        payload.put("timestamp", "2026-05-21T12:00:00Z");

        IncidentNatsEvent event = new IncidentNatsEvent("incident.updated", payload);

        listener.handleIncidentNatsEvent(event);

        Mockito.verify(publisher).publish("incident.updated", payload);
    }
}
