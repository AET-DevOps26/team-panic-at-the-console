package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.openapitools.api.NotificationsApi;
import org.openapitools.model.NotificationListResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies public notification REST routes to notification-service.
 */
@RestController
class NotificationsProxyController implements NotificationsApi {

    private final RestClient notificationServiceClient;

    NotificationsProxyController(
            @Qualifier("notificationServiceClient") RestClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    public ResponseEntity<NotificationListResponse> listNotifications(
            UUID recipientId, Boolean unreadOnly, Integer page, Integer size) {
        return DownstreamProxy.get(
                notificationServiceClient,
                listNotificationsPath(recipientId, unreadOnly, page, size),
                NotificationListResponse.class);
    }

    @Override
    public ResponseEntity<Void> markNotificationRead(UUID notificationId) {
        return DownstreamProxy.post(
                notificationServiceClient,
                "/notifications/{notificationId}/read",
                Void.class,
                notificationId);
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsRead(UUID recipientId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/notifications/read-all");
        if (recipientId != null) builder.queryParam("recipientId", recipientId);
        return DownstreamProxy.post(
                notificationServiceClient, builder.build().toUriString(), Void.class);
    }

    private static String listNotificationsPath(
            UUID recipientId, Boolean unreadOnly, Integer page, Integer size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/notifications");
        if (recipientId != null) builder.queryParam("recipientId", recipientId);
        if (unreadOnly != null) builder.queryParam("unreadOnly", unreadOnly);
        if (page != null) builder.queryParam("page", page);
        if (size != null) builder.queryParam("size", size);
        return builder.build().toUriString();
    }
}
