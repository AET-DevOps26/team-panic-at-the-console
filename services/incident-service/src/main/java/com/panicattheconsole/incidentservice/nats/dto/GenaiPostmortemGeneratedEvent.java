package com.panicattheconsole.incidentservice.nats.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenaiPostmortemGeneratedEvent {

    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("rootCause")
    private String rootCause;

    @JsonProperty("timeline")
    private List<String> timeline;

    @JsonProperty("actionItems")
    private List<String> actionItems;

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public List<String> getTimeline() { return timeline; }
    public void setTimeline(List<String> timeline) { this.timeline = timeline; }

    public List<String> getActionItems() { return actionItems; }
    public void setActionItems(List<String> actionItems) { this.actionItems = actionItems; }
}
