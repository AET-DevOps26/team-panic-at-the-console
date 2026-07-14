package com.panicattheconsole.notificationservice.api;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.openapitools.api.NotificationsApi;
import org.openapitools.model.NotificationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST surface for notifications, implementing the generated {@link NotificationsApi}
 * contract from api/openapi.yaml. Translates the domain entity into the generated API
 * model so the spec stays the single source of truth.
 *
 * <p>All endpoints are scoped to the calling user, identified by the X-User-Id
 * header the gateway injects after validating the session (ADR 0007).
 */
@RestController
public class NotificationController implements NotificationsApi {

    /** Validated session identity injected by the gateway (ADR 0007). */
    static final String USER_ID_HEADER = "X-User-Id";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final NotificationService notificationService;
    private final HttpServletRequest httpRequest;

    public NotificationController(NotificationService notificationService, HttpServletRequest httpRequest) {
        this.notificationService = notificationService;
        this.httpRequest = httpRequest;
    }

    @Override
    public ResponseEntity<NotificationListResponse> listNotifications(
            Boolean unreadOnly, Integer page, Integer size) {

        UUID userId = sessionUserId();
        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        boolean unread = Boolean.TRUE.equals(unreadOnly);

        Page<Notification> result = notificationService.list(userId, unread, safePage, safeSize);

        Set<UUID> readIds = notificationService.readIdsFor(
                userId,
                result.getContent().stream().map(Notification::getId).toList());

        List<org.openapitools.model.Notification> items = result.getContent().stream()
                .map(n -> toApiModel(n, readIds.contains(n.getId())))
                .toList();

        NotificationListResponse body = new NotificationListResponse(
                items,
                Math.toIntExact(result.getTotalElements()),
                safePage,
                safeSize,
                Math.toIntExact(notificationService.unreadCount(userId)));

        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Void> markNotificationRead(UUID notificationId) {
        return notificationService.markRead(notificationId, sessionUserId())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsRead() {
        notificationService.markAllRead(sessionUserId());
        return ResponseEntity.noContent().build();
    }

    private UUID sessionUserId() {
        String header = httpRequest.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing " + USER_ID_HEADER + " header");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + USER_ID_HEADER + " header");
        }
    }

    private static org.openapitools.model.Notification toApiModel(Notification n, boolean read) {
        return new org.openapitools.model.Notification(
                n.getId(),
                n.getIncidentId(),
                org.openapitools.model.NotificationType.valueOf(n.getType().name()),
                n.getMessage(),
                read,
                n.getCreatedAt().atOffset(ZoneOffset.UTC))
                .recipientId(n.getRecipientId());
    }
}
