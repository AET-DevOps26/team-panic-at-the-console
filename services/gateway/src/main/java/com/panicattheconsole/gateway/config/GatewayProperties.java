package com.panicattheconsole.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private String incidentServiceUrl = "http://localhost:8081";
    private String genaiServiceUrl = "http://localhost:8087";

    public String getIncidentServiceUrl() {
        return incidentServiceUrl;
    }

    public void setIncidentServiceUrl(String incidentServiceUrl) {
        this.incidentServiceUrl = incidentServiceUrl;
    }

    public String getGenaiServiceUrl() {
        return genaiServiceUrl;
    }

    public void setGenaiServiceUrl(String genaiServiceUrl) {
        this.genaiServiceUrl = genaiServiceUrl;
    }
}
