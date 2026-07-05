package com.panicattheconsole.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", matchIfMissing = true)
class ServiceClientsConfig {

    @Bean
    RestClient incidentServiceClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.getIncidentServiceUrl()).build();
    }

    @Bean
    RestClient userServiceClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.getUserServiceUrl()).build();
    }

    @Bean
    RestClient notificationServiceClient(GatewayProperties properties) {
        return RestClient.builder().baseUrl(properties.getNotificationServiceUrl()).build();
    }
}
