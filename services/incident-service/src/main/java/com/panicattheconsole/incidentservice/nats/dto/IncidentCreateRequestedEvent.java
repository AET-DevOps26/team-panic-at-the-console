package com.panicattheconsole.incidentservice.nats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IncidentCreateRequestedEvent {

    @JsonProperty("sourceId")
    private String sourceId;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("timestamp")
    private String timestamp;

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
