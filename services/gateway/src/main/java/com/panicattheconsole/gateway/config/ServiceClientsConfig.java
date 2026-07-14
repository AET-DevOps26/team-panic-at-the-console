package com.panicattheconsole.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.panicattheconsole.gateway.auth.IdentityHeaderRelay;

@Configuration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", matchIfMissing = true)
class ServiceClientsConfig {

    @Bean
    RestClient incidentServiceClient(GatewayProperties properties, IdentityHeaderRelay relay) {
        return RestClient.builder()
                .baseUrl(properties.getIncidentServiceUrl())
                .requestInterceptor(relay)
                .build();
    }

    @Bean
    RestClient eventServiceClient(GatewayProperties properties, IdentityHeaderRelay relay) {
        return RestClient.builder()
                .baseUrl(properties.getEventServiceUrl())
                .requestInterceptor(relay)
                .build();
    }

    @Bean
    RestClient userServiceClient(GatewayProperties properties, IdentityHeaderRelay relay) {
        return RestClient.builder()
                .baseUrl(properties.getUserServiceUrl())
                .requestInterceptor(relay)
                .build();
    }

    @Bean
    RestClient notificationServiceClient(GatewayProperties properties, IdentityHeaderRelay relay) {
        return RestClient.builder()
                .baseUrl(properties.getNotificationServiceUrl())
                .requestInterceptor(relay)
                .build();
    }

    @Bean
    RestClient webhookServiceClient(GatewayProperties properties, IdentityHeaderRelay relay) {
        return RestClient.builder()
                .baseUrl(properties.getWebhookServiceUrl())
                .requestInterceptor(relay)
                .build();
    }
}
