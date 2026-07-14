package com.panicattheconsole.webhookservice.config;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ingestion and publish settings.
 *
 * @param secrets per-source HMAC secrets, keyed by the {@code {source}} path
 *     segment (relaxed binding: {@code WEBHOOK_SECRETS_GITHUB=...} configures
 *     the {@code github} source). A source with a secret must send a valid
 *     {@code X-Hub-Signature-256} header.
 * @param requireSignature when true, requests for sources without a
 *     configured secret are rejected instead of accepted unverified.
 * @param publish retry policy for events whose NATS publish failed.
 */
@ConfigurationProperties(prefix = "webhook")
public record WebhookProperties(Map<String, String> secrets, boolean requireSignature, Publish publish) {

    public WebhookProperties {
        // Deployments template secrets in unconditionally (e.g. the Helm
        // webhook-credentials secret); a blank value means "not configured",
        // never "HMAC with an empty key".
        secrets = secrets == null ? Map.of()
                : secrets.entrySet().stream()
                        .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        publish = publish == null ? new Publish(null, null) : publish;
    }

    /**
     * @param minAgeMs events younger than this are skipped by the retrier so
     *     it never races the in-request publish attempt
     * @param maxAttempts failed events are abandoned (with an error log) after
     *     this many publish attempts
     */
    public record Publish(Long minAgeMs, Integer maxAttempts) {

        public Publish {
            minAgeMs = minAgeMs == null ? 10_000L : minAgeMs;
            maxAttempts = maxAttempts == null ? 10 : maxAttempts;
        }
    }
}
