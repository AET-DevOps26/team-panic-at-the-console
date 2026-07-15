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
    private String eventServiceUrl;

    @NotBlank
    private String userServiceUrl;

    @NotBlank
    private String notificationServiceUrl;

    @NotBlank
    private String webhookServiceUrl;

    public String getIncidentServiceUrl() {
        return incidentServiceUrl;
    }

    public void setIncidentServiceUrl(String incidentServiceUrl) {
        this.incidentServiceUrl = incidentServiceUrl;
    }

    public String getEventServiceUrl() {
        return eventServiceUrl;
    }

    public void setEventServiceUrl(String eventServiceUrl) {
        this.eventServiceUrl = eventServiceUrl;
    }

    public String getUserServiceUrl() {
        return userServiceUrl;
    }

    public void setUserServiceUrl(String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
    }

    public String getNotificationServiceUrl() {
        return notificationServiceUrl;
    }

    public void setNotificationServiceUrl(String notificationServiceUrl) {
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public String getWebhookServiceUrl() {
        return webhookServiceUrl;
    }

    public void setWebhookServiceUrl(String webhookServiceUrl) {
        this.webhookServiceUrl = webhookServiceUrl;
    }
}
