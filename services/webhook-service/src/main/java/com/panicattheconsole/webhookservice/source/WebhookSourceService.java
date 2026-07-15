package com.panicattheconsole.webhookservice.source;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.panicattheconsole.webhookservice.event.ExternalEventRepository;

@Service
public class WebhookSourceService {

    /** 256-bit secrets, hex-encoded to 64 chars (the column length). */
    private static final int SECRET_BYTES = 32;

    private final WebhookSourceRepository repository;
    private final ExternalEventRepository eventRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /** Thrown on create when the slug is already registered (maps to 409). */
    public static class SlugTakenException extends RuntimeException {
        SlugTakenException(String slug) {
            super("a source with slug '" + slug + "' is already registered");
        }
    }

    WebhookSourceService(WebhookSourceRepository repository, ExternalEventRepository eventRepository,
            Clock clock) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    public List<WebhookSource> list() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    /**
     * Receipt time of the newest external event per source slug, for the
     * "is my webhook working" signal on the Sources page. Includes slugs that
     * were never registered (env-secret or unverified sources).
     */
    public Map<String, Instant> lastEventBySource() {
        return eventRepository.findLastReceivedBySource().stream()
                .collect(Collectors.toMap(
                        ExternalEventRepository.SourceLastEvent::getSource,
                        ExternalEventRepository.SourceLastEvent::getLastReceivedAt));
    }

    public WebhookSource create(String slug) {
        if (repository.existsById(slug)) {
            throw new SlugTakenException(slug);
        }
        try {
            return repository.save(new WebhookSource(slug, generateSecret(), clock.instant()));
        } catch (DataIntegrityViolationException e) {
            // Concurrent create hit the primary key between our lookup and insert.
            throw new SlugTakenException(slug);
        }
    }

    public Optional<WebhookSource> rotateSecret(String slug) {
        return repository.findById(slug).map(source -> {
            source.rotateSecret(generateSecret(), clock.instant());
            return repository.save(source);
        });
    }

    /** Deletes the registration; the source's external events are kept (ADR 0008). */
    public boolean delete(String slug) {
        if (!repository.existsById(slug)) {
            return false;
        }
        repository.deleteById(slug);
        return true;
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
