package com.panicattheconsole.notificationservice.api;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.openapitools.api.NotificationsApi;
import org.openapitools.model.NotificationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.service.NotificationService;

/**
 * REST surface for notifications, implementing the generated {@link NotificationsApi}
 * contract from api/openapi.yaml. Translates the domain entity into the generated API
 * model so the spec stays the single source of truth.
 */
@RestController
public class NotificationController implements NotificationsApi {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<NotificationListResponse> listNotifications(
            UUID recipientId, Boolean unreadOnly, Integer page, Integer size) {

        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        boolean unread = Boolean.TRUE.equals(unreadOnly);

        Page<Notification> result = notificationService.list(recipientId, unread, safePage, safeSize);

        List<org.openapitools.model.Notification> items = result.getContent().stream()
                .map(NotificationController::toApiModel)
                .toList();

        NotificationListResponse body = new NotificationListResponse(
                items,
                Math.toIntExact(result.getTotalElements()),
                safePage,
                safeSize,
                Math.toIntExact(notificationService.unreadCount(recipientId)));

        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Void> markNotificationRead(UUID notificationId) {
        return notificationService.markRead(notificationId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsRead(UUID recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.noContent().build();
    }

    private static org.openapitools.model.Notification toApiModel(Notification n) {
        return new org.openapitools.model.Notification(
                n.getId(),
                n.getIncidentId(),
                org.openapitools.model.NotificationType.valueOf(n.getType().name()),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt().atOffset(ZoneOffset.UTC))
                .recipientId(n.getRecipientId());
    }
}
