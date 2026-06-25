package com.panicattheconsole.incidentservice.nats.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenaiSolutionsGeneratedEvent {

    @JsonProperty("incidentId")
    private String incidentId;

    @JsonProperty("solutions")
    private List<String> solutions;

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public List<String> getSolutions() { return solutions; }
    public void setSolutions(List<String> solutions) { this.solutions = solutions; }
}
