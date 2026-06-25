package com.panicattheconsole.incidentservice.nats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IncidentSeverityEscalateRequestedEvent {

    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("requestedSeverity")
    private String requestedSeverity;

    @JsonProperty("timestamp")
    private String timestamp;

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getRequestedSeverity() {
        return requestedSeverity;
    }

    public void setRequestedSeverity(String requestedSeverity) {
        this.requestedSeverity = requestedSeverity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
