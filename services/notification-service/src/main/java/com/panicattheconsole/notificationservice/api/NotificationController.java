package com.panicattheconsole.notificationservice.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * List notifications, newest first. When {@code recipientId} is given the result
     * is scoped to that user (their personal notifications plus broadcasts); otherwise
     * all notifications are returned.
     */
    @GetMapping
    public NotificationListResponse list(
            @RequestParam(required = false) UUID recipientId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;

        Page<Notification> result = notificationService.list(recipientId, unreadOnly, safePage, safeSize);

        List<NotificationResponse> items = result.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return new NotificationListResponse(
                items,
                result.getTotalElements(),
                safePage,
                safeSize,
                notificationService.unreadCount(recipientId));
    }

    /** Mark a single notification as read. */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID notificationId) {
        boolean found = notificationService.markRead(notificationId);
        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Mark all notifications visible to the recipient (or all, when omitted) as read. */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestParam(required = false) UUID recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.noContent().build();
    }
}
