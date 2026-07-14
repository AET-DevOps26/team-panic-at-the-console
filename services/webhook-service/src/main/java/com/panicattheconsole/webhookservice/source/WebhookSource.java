package com.panicattheconsole.webhookservice.source;

import java.time.Instant;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered webhook source: a slug senders deliver to
 * ({@code POST /webhooks/{slug}}) plus the HMAC secret their deliveries must
 * be signed with. The secret is generated server-side and returned to the user
 * only on create/rotate; it stays stored because HMAC verification needs the
 * original value (unlike a password it cannot be hashed).
 */
@Entity
@Table(name = "webhook_sources")
public class WebhookSource {

    /**
     * Also enforced on ingest ({@code /webhooks/{source}}); the tight charset
     * keeps slugs safe as metrics label values. Mirrors the OpenAPI pattern.
     */
    public static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    @Id
    @Column(length = 64)
    private String slug;

    @Column(nullable = false, length = 64)
    private String secret;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "secret_rotated_at")
    private Instant secretRotatedAt;

    protected WebhookSource() {
        // for JPA
    }

    public WebhookSource(String slug, String secret, Instant createdAt) {
        this.slug = slug;
        this.secret = secret;
        this.createdAt = createdAt;
    }

    public void rotateSecret(String newSecret, Instant at) {
        this.secret = newSecret;
        this.secretRotatedAt = at;
    }

    public String getSlug() {
        return slug;
    }

    public String getSecret() {
        return secret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSecretRotatedAt() {
        return secretRotatedAt;
    }
}
