package com.panicattheconsole.incidentservice.nats;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;
import io.nats.client.Nats;

@Configuration
public class NatsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NatsConfiguration.class);

    @Value("${nats.server:nats://localhost:4222}")
    private String natsServer;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        log.info("Connecting to NATS at {}", natsServer);
        try {
            Connection connection = Nats.connect(natsServer);
            log.info("Successfully connected to NATS");
            return connection;
        } catch (Exception e) {
            log.error("Failed to connect to NATS at {}. Events will not be published.", natsServer, e);
            throw new RuntimeException("Failed to connect to NATS", e);
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
