package com.panicattheconsole.gateway.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", havingValue = "false")
public class MockDownstreamClientsConfig {

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

    private record ClientPair(RestClient client, MockRestServiceServer server) {}
}
