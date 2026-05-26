package com.panicattheconsole.gateway.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@TestConfiguration
@ConditionalOnProperty(name = "gateway.downstream-clients.enabled", havingValue = "false")
public class MockDownstreamClientsConfig {

    private final ClientPair incident = ClientPair.create("http://localhost:8081");
    private final ClientPair genai = ClientPair.create("http://localhost:8087");

    @Bean
    RestClient incidentServiceClient() {
        return incident.client();
    }

    @Bean
    MockRestServiceServer incidentServer() {
        return incident.server();
    }

    @Bean
    RestClient genaiServiceClient() {
        return genai.client();
    }

    @Bean
    MockRestServiceServer genaiServer() {
        return genai.server();
    }

    private record ClientPair(RestClient client, MockRestServiceServer server) {
        static ClientPair create(String baseUrl) {
            RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            return new ClientPair(builder.build(), server);
        }
    }
}
