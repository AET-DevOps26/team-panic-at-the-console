package com.panicattheconsole.webhookservice.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.panicattheconsole.webhookservice.config.WebhookProperties;
import com.panicattheconsole.webhookservice.ingest.SignatureVerifier.Decision;
import com.panicattheconsole.webhookservice.source.WebhookSource;
import com.panicattheconsole.webhookservice.source.WebhookSourceRepository;

class SignatureVerifierTest {

    // Test vector from the GitHub webhook documentation, so validation is
    // checked against an independently computed HMAC.
    private static final String SECRET = "It's a Secret to Everybody";
    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String VALID_SIGNATURE =
            "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    private static SignatureVerifier verifier(Map<String, String> secrets, boolean requireSignature) {
        return verifier(secrets, requireSignature, mock(WebhookSourceRepository.class));
    }

    private static SignatureVerifier verifier(
            Map<String, String> secrets, boolean requireSignature, WebhookSourceRepository repository) {
        return new SignatureVerifier(new WebhookProperties(secrets, requireSignature, null), repository);
    }

    private static WebhookSourceRepository registered(String slug, String secret) {
        WebhookSourceRepository repository = mock(WebhookSourceRepository.class);
        doReturn(Optional.of(new WebhookSource(slug, secret, Instant.EPOCH)))
                .when(repository).findById(slug);
        return repository;
    }

    @Test
    void acceptsUnverifiedWhenNoSecretConfigured() {
        Decision decision = verifier(Map.of(), false).check("github", null, BODY);
        assertThat(decision).isEqualTo(new Decision.Accepted(false));
    }

    @Test
    void rejectsSecretlessSourceInRequireSignatureMode() {
        Decision decision = verifier(Map.of(), true).check("github", VALID_SIGNATURE, BODY);
        assertThat(decision).isInstanceOf(Decision.Rejected.class);
    }

    @Test
    void acceptsValidSignature() {
        Decision decision = verifier(Map.of("github", SECRET), false).check("github", VALID_SIGNATURE, BODY);
        assertThat(decision).isEqualTo(new Decision.Accepted(true));
    }

    @Test
    void acceptsUppercaseHexSignature() {
        String uppercased = "sha256=" + VALID_SIGNATURE.substring("sha256=".length()).toUpperCase(Locale.ROOT);
        Decision decision = verifier(Map.of("github", SECRET), false).check("github", uppercased, BODY);
        assertThat(decision).isEqualTo(new Decision.Accepted(true));
    }

    @Test
    void rejectsWrongSignature() {
        String wrong = "sha256=" + "0".repeat(64);
        Decision decision = verifier(Map.of("github", SECRET), false).check("github", wrong, BODY);
        assertThat(decision).isEqualTo(new Decision.Rejected("signature mismatch"));
    }

    @Test
    void rejectsTamperedBody() {
        byte[] tampered = "Hello, World".getBytes(StandardCharsets.UTF_8);
        Decision decision = verifier(Map.of("github", SECRET), false).check("github", VALID_SIGNATURE, tampered);
        assertThat(decision).isEqualTo(new Decision.Rejected("signature mismatch"));
    }

    @Test
    void rejectsMissingSignatureWhenSecretConfigured() {
        SignatureVerifier verifier = verifier(Map.of("github", SECRET), false);
        assertThat(verifier.check("github", null, BODY)).isInstanceOf(Decision.Rejected.class);
        assertThat(verifier.check("github", "  ", BODY)).isInstanceOf(Decision.Rejected.class);
    }

    @Test
    void rejectsUnsupportedScheme() {
        Decision decision = verifier(Map.of("github", SECRET), false)
                .check("github", "sha1=abcdef", BODY);
        assertThat(decision).isInstanceOf(Decision.Rejected.class);
    }

    @Test
    void blankSecretMeansUnconfiguredNotEmptyKey() {
        // Helm templates the secret env var in even when unset; a blank value
        // must behave exactly like no secret at all.
        assertThat(verifier(Map.of("github", ""), false).check("github", null, BODY))
                .isEqualTo(new Decision.Accepted(false));
        assertThat(verifier(Map.of("github", "  "), true).check("github", VALID_SIGNATURE, BODY))
                .isInstanceOf(Decision.Rejected.class);
    }

    @Test
    void secretsAreScopedPerSource() {
        // gitlab has no secret; github's secret must not apply to it
        SignatureVerifier verifier = verifier(Map.of("github", SECRET), false);
        assertThat(verifier.check("gitlab", null, BODY)).isEqualTo(new Decision.Accepted(false));
    }

    @Test
    void registeredSourceVerifiesWithItsStoredSecret() {
        SignatureVerifier verifier = verifier(Map.of(), false, registered("github", SECRET));
        assertThat(verifier.check("github", VALID_SIGNATURE, BODY)).isEqualTo(new Decision.Accepted(true));
    }

    @Test
    void registeredSourceRequiresSignatureEvenWhenSignaturesAreOptional() {
        SignatureVerifier verifier = verifier(Map.of(), false, registered("github", SECRET));
        assertThat(verifier.check("github", null, BODY)).isInstanceOf(Decision.Rejected.class);
    }

    @Test
    void registeredSecretTakesPrecedenceOverEnvFallback() {
        SignatureVerifier verifier =
                verifier(Map.of("github", "stale-env-secret"), false, registered("github", SECRET));
        assertThat(verifier.check("github", VALID_SIGNATURE, BODY)).isEqualTo(new Decision.Accepted(true));
    }
}
