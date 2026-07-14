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
 * Proxies public notification REST routes to notification-service. The caller
 * is identified downstream by the X-User-Id header the IdentityHeaderRelay
 * injects from the validated session; no client-supplied scoping params.
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
            Boolean unreadOnly, Integer page, Integer size) {
        return DownstreamProxy.get(
                notificationServiceClient,
                listNotificationsPath(unreadOnly, page, size),
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
    public ResponseEntity<Void> markAllNotificationsRead() {
        return DownstreamProxy.post(
                notificationServiceClient, "/notifications/read-all", Void.class);
    }

    private static String listNotificationsPath(Boolean unreadOnly, Integer page, Integer size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/notifications");
        if (unreadOnly != null) builder.queryParam("unreadOnly", unreadOnly);
        if (page != null) builder.queryParam("page", page);
        if (size != null) builder.queryParam("size", size);
        return builder.build().toUriString();
    }
}
