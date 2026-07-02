package com.panicattheconsole.notificationservice.api;

import java.util.List;

/**
 * Paged list of notifications. {@code unreadCount} covers the same scope as the
 * query (the recipient's visible notifications, or all when unfiltered) so the
 * frontend can render an unread badge without a second request.
 */
public record NotificationListResponse(
        List<NotificationResponse> items,
        long total,
        int page,
        int size,
        long unreadCount) {
}
