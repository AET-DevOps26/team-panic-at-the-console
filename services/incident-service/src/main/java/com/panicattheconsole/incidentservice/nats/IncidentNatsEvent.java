package com.panicattheconsole.incidentservice.nats;

import java.util.Map;

/**
 * Domain event representing a NATS payload that should be sent after transaction commit.
 */
public class IncidentNatsEvent {

    private final String subject;
    private final Map<String, Object> payload;

    public IncidentNatsEvent(String subject, Map<String, Object> payload) {
        this.subject = subject;
        this.payload = payload;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
