package com.panicattheconsole.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", matchIfMissing = true)
class ServiceClientsConfig {

    @Bean
    RestClient incidentServiceClient(GatewayProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getIncidentServiceUrl()).build();
    }
}
