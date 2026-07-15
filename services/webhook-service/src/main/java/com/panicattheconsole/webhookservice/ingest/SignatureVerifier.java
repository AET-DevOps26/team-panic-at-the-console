package com.panicattheconsole.webhookservice.ingest;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.panicattheconsole.webhookservice.config.WebhookProperties;
import com.panicattheconsole.webhookservice.source.WebhookSource;
import com.panicattheconsole.webhookservice.source.WebhookSourceRepository;

/**
 * Decides whether an incoming webhook request may be ingested. All signature
 * policy lives here: which sources need a signature, the HMAC scheme, and the
 * comparison. Callers only branch on the returned {@link Decision}.
 *
 * <p>The secret for a source comes from its registration (self-service via the
 * Sources page) or, for unregistered slugs, from the env-configured
 * deployment fallback ({@code WEBHOOK_SECRETS_<SOURCE>}). A registered source
 * always has a secret, so its deliveries are always verified regardless of the
 * require-signature mode.
 *
 * <p>The scheme is the GitHub convention: {@code X-Hub-Signature-256:
 * sha256=<hex HMAC-SHA256 of the raw request body>}, keyed with the secret
 * configured for the source.
 */
@Component
public class SignatureVerifier {

    public static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private static final String SCHEME_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public sealed interface Decision {
        /** Ingest the request; {@code verified} is false for secretless sources. */
        record Accepted(boolean verified) implements Decision {}

        /** Reject the request with 401; {@code reason} is safe to return to the caller. */
        record Rejected(String reason) implements Decision {}
    }

    private final WebhookProperties properties;
    private final WebhookSourceRepository sourceRepository;

    SignatureVerifier(WebhookProperties properties, WebhookSourceRepository sourceRepository) {
        this.properties = properties;
        this.sourceRepository = sourceRepository;
    }

    public Decision check(String source, String signatureHeader, byte[] rawBody) {
        String secret = sourceRepository.findById(source)
                .map(WebhookSource::getSecret)
                .orElseGet(() -> properties.secrets().get(source));
        if (secret == null) {
            return properties.requireSignature()
                    ? new Decision.Rejected("signature required but no secret is configured for this source")
                    : new Decision.Accepted(false);
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return new Decision.Rejected("missing " + SIGNATURE_HEADER + " header");
        }
        if (!signatureHeader.startsWith(SCHEME_PREFIX)) {
            return new Decision.Rejected("unsupported signature scheme, expected " + SCHEME_PREFIX + "<hex>");
        }
        String provided = signatureHeader.substring(SCHEME_PREFIX.length()).toLowerCase(Locale.ROOT);
        String expected = hmacSha256Hex(secret, rawBody);
        // Constant-time comparison: a plain equals would leak match length.
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII));
        return valid ? new Decision.Accepted(true) : new Decision.Rejected("signature mismatch");
    }

    private static String hmacSha256Hex(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
