package com.panicattheconsole.incidentservice.nats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenaiSummaryGeneratedEvent {

    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("summary")
    private String summary;

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
