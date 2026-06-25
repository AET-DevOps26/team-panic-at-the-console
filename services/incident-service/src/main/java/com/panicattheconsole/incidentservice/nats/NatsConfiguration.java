package com.panicattheconsole.incidentservice.nats;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.nats.client.Connection;
import io.nats.client.Nats;

@Configuration
public class NatsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NatsConfiguration.class);

    @Value("${nats.url:nats://localhost:4222}")
    private String natsServer;

    @Value("${nats.failOnStartup:false}")
    private boolean failOnStartup;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        log.info("Attempting to connect to NATS at {}", natsServer);
        try {
            Connection connection = Nats.connect(natsServer);
            log.info("Successfully connected to NATS");
            return connection;
        } catch (Exception e) {
            log.warn(
                    "Failed to connect to NATS at {}. Continuing with degraded mode (events may not be published/consumed).",
                    natsServer, e);
            if (failOnStartup) {
                log.error("nats.failOnStartup is enabled; aborting service startup");
                throw new RuntimeException("Failed to connect to NATS; service startup aborted", e);
            }
            return null;
        }
    }

}
