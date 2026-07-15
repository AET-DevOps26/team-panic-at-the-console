package com.panicattheconsole.webhookservice.publish;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

import jakarta.annotation.PreDestroy;

/**
 * Owns the NATS connection lifecycle. Connecting lazily (instead of a startup
 * {@code Connection} bean) means a NATS outage never takes the service down:
 * webhooks keep being accepted and persisted, and publishing resumes via the
 * retrier once NATS is reachable again.
 */
@Component
public class NatsConnectionFactory {

    private final String url;
    private Connection connection;

    NatsConnectionFactory(@Value("${nats.url}") String url) {
        this.url = url;
    }

    /** Returns a live connection, establishing one if needed. */
    public synchronized Connection get() throws IOException, InterruptedException {
        if (connection == null || connection.getStatus() == Connection.Status.CLOSED) {
            Options options = new Options.Builder()
                    .server(url)
                    .connectionName("webhook-service")
                    .connectionTimeout(Duration.ofSeconds(2))
                    .maxReconnects(-1)
                    .build();
            connection = Nats.connect(options);
        }
        return connection;
    }

    @PreDestroy
    synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
