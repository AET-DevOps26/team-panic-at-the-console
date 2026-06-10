package com.panicattheconsole.incidentservice.nats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenaiSeverityGeneratedEvent {

    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("reason")
    private String reason;

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
