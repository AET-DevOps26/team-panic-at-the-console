package com.panicattheconsole.webhookservice.source;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.openapitools.api.WebhookSourcesApi;
import org.openapitools.model.CreateWebhookSourceRequest;
import org.openapitools.model.ErrorResponse;
import org.openapitools.model.WebhookSourceListResponse;
import org.openapitools.model.WebhookSourceWithSecret;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service source management, serving the generated `webhook-sources` API.
 * Reached by users through the gateway proxy (session-authenticated there).
 * The generated secret leaves the service only in the create/rotate response;
 * list responses never carry it.
 */
@RestController
class WebhookSourceController implements WebhookSourcesApi {

    private final WebhookSourceService sourceService;

    WebhookSourceController(WebhookSourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public ResponseEntity<WebhookSourceListResponse> listWebhookSources() {
        Map<String, Instant> lastEvents = sourceService.lastEventBySource();
        return ResponseEntity.ok(new WebhookSourceListResponse()
                .items(sourceService.list().stream()
                        .map(source -> new org.openapitools.model.WebhookSource()
                                .slug(source.getSlug())
                                .createdAt(atUtc(source.getCreatedAt()))
                                .secretRotatedAt(atUtc(source.getSecretRotatedAt()))
                                .lastEventAt(atUtc(lastEvents.get(source.getSlug()))))
                        .toList()));
    }

    @Override
    public ResponseEntity<WebhookSourceWithSecret> createWebhookSource(CreateWebhookSourceRequest request) {
        // Bean Validation on the generated interface is not active here, so
        // enforce the slug pattern defensively (same rule as the ingest route).
        String slug = request.getSlug();
        if (slug == null || !WebhookSource.SLUG_PATTERN.matcher(slug).matches()) {
            throw new InvalidSlugException();
        }
        WebhookSource created = sourceService.create(slug);
        return ResponseEntity.status(HttpStatus.CREATED).body(toWithSecret(created));
    }

    @Override
    public ResponseEntity<WebhookSourceWithSecret> rotateWebhookSourceSecret(String source) {
        return sourceService.rotateSecret(source)
                .map(rotated -> ResponseEntity.ok(toWithSecret(rotated)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteWebhookSource(String source) {
        return sourceService.delete(source)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static WebhookSourceWithSecret toWithSecret(WebhookSource source) {
        return new WebhookSourceWithSecret()
                .slug(source.getSlug())
                .secret(source.getSecret())
                .createdAt(atUtc(source.getCreatedAt()))
                .secretRotatedAt(atUtc(source.getSecretRotatedAt()));
    }

    private static OffsetDateTime atUtc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static class InvalidSlugException extends RuntimeException {}

    @ExceptionHandler(InvalidSlugException.class)
    ResponseEntity<ErrorResponse> onInvalidSlug(InvalidSlugException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid slug: expected a lowercase slug"));
    }

    @ExceptionHandler(WebhookSourceService.SlugTakenException.class)
    ResponseEntity<ErrorResponse> onSlugTaken(WebhookSourceService.SlugTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }
}
