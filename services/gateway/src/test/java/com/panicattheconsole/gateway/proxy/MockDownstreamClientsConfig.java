package com.panicattheconsole.gateway.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", havingValue = "false")
public class MockDownstreamClientsConfig {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    ClientPair incidentClientPair(RestClient.Builder restClientBuilder) {
        RestClient.Builder builder = restClientBuilder.clone().baseUrl("http://localhost:8081");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new ClientPair(builder.build(), server);
    }

    @Bean
    RestClient incidentServiceClient(ClientPair incidentClientPair) {
        return incidentClientPair.client();
    }

    @Bean
    MockRestServiceServer incidentServer(ClientPair incidentClientPair) {
        return incidentClientPair.server();
    }

    @Bean
    ClientPair userClientPair(RestClient.Builder restClientBuilder) {
        RestClient.Builder builder = restClientBuilder.clone().baseUrl("http://localhost:8084");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new ClientPair(builder.build(), server);
    }

    @Bean
    RestClient userServiceClient(ClientPair userClientPair) {
        return userClientPair.client();
    }

    @Bean
    MockRestServiceServer userServer(ClientPair userClientPair) {
        return userClientPair.server();
    }

    @Bean
    ClientPair notificationClientPair(RestClient.Builder restClientBuilder) {
        RestClient.Builder builder = restClientBuilder.clone().baseUrl("http://localhost:8085");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new ClientPair(builder.build(), server);
    }

    @Bean
    RestClient notificationServiceClient(ClientPair notificationClientPair) {
        return notificationClientPair.client();
    }

    @Bean
    MockRestServiceServer notificationServer(ClientPair notificationClientPair) {
        return notificationClientPair.server();
    }

    private record ClientPair(RestClient client, MockRestServiceServer server) {}
}
