package com.panicattheconsole.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    @NotBlank
    private String incidentServiceUrl;

    @NotBlank
    private String userServiceUrl;

    public String getIncidentServiceUrl() {
        return incidentServiceUrl;
    }

    public void setIncidentServiceUrl(String incidentServiceUrl) {
        this.incidentServiceUrl = incidentServiceUrl;
    }

    public String getUserServiceUrl() {
        return userServiceUrl;
    }

    public void setUserServiceUrl(String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
    }
}
