package com.panicattheconsole.notificationservice.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.domain.NotificationType;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.repository.NotificationReadRepository;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final int COMMENT_PREVIEW_LENGTH = 120;

    private final NotificationRepository repository;
    private final NotificationReadRepository readRepository;

    public NotificationService(
            NotificationRepository repository,
            NotificationReadRepository readRepository) {
        this.repository = repository;
        this.readRepository = readRepository;
    }

    /**
     * Turn an incident event into stored notifications.
     *
     * <p>Targeting: incident.created is a broadcast to everyone;
     * incident.assigned targets the assigned user; severity, status, and
     * comment events fan out to the incident's assignees. The acting user is
     * never notified about their own action (fan-outs skip the actor here,
     * broadcasts are filtered on the read side).
     *
     * <p>Returns the saved notifications, empty for subjects this service does
     * not notify on or when nobody is left to notify.
     */
    @Transactional
    public List<Notification> record(IncidentEvent event) {

        List<Notification> notifications = switch (event.subject()) {

            case "incident.created" -> List.of(new Notification(
                    event.incidentId(),
                    NotificationType.INCIDENT_CREATED,
                    null,
                    createdMessage(event),
                    event.timestamp(),
                    event.actorId()));

            case "incident.severity.escalated" -> fanOutToAssignees(
                    event,
                    NotificationType.SEVERITY_ESCALATED,
                    "Incident severity changed to " + event.newSeverity() + ".");

            case "incident.status.changed" -> "resolved".equals(event.newStatus())
                    ? fanOutToAssignees(
                            event,
                            NotificationType.INCIDENT_RESOLVED,
                            "An incident you are assigned to was resolved.")
                    : fanOutToAssignees(
                            event,
                            NotificationType.STATUS_CHANGED,
                            "Incident status changed to " + event.newStatus() + ".");

            case "incident.comment.added" -> fanOutToAssignees(
                    event,
                    NotificationType.COMMENT_ADDED,
                    commentMessage(event));

            case "incident.assigned" -> event.assignedUserId().equals(event.actorId())
                    ? List.<Notification>of()
                    : List.of(new Notification(
                            event.incidentId(),
                            NotificationType.INCIDENT_ASSIGNED,
                            event.assignedUserId(),
                            "You were assigned to an incident.",
                            event.timestamp(),
                            event.actorId()));

            default -> null;
        };

        if (notifications == null) {
            log.warn("No notification mapping for subject {}; ignoring", event.subject());
            return List.of();
        }
        if (notifications.isEmpty()) {
            return List.of();
        }

        List<Notification> saved = repository.saveAll(notifications);
        log.debug("Stored {} {} notification(s) for incident {}",
                saved.size(), saved.get(0).getType(), event.incidentId());
        return saved;
    }

    /** One personal notification per assignee, skipping the acting user. */
    private static List<Notification> fanOutToAssignees(
            IncidentEvent event,
            NotificationType type,
            String message) {
        if (event.assignedUserIds() == null) {
            return List.of();
        }
        return event.assignedUserIds().stream()
                .filter(userId -> !userId.equals(event.actorId()))
                .map(userId -> new Notification(
                        event.incidentId(),
                        type,
                        userId,
                        message,
                        event.timestamp(),
                        event.actorId()))
                .toList();
    }

    private static String createdMessage(IncidentEvent event) {
        if (event.title() == null) {
            return "A new incident was opened.";
        }
        return event.severity() == null
                ? "New incident: " + event.title()
                : "New incident: " + event.title() + " (" + event.severity() + ")";
    }

    private static String commentMessage(IncidentEvent event) {
        if (event.content() == null || event.content().isBlank()) {
            return "A new comment was added to an incident.";
        }
        return "New comment: \"" + truncate(event.content().strip(), COMMENT_PREVIEW_LENGTH) + "\"";
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, boolean unreadOnly, int page, int size) {
        // Secondary id key keeps pagination stable when notifications share a createdAt.
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return unreadOnly
                ? repository.findVisibleToUnread(userId, pageable)
                : repository.findVisibleTo(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countVisibleToUnread(userId);
    }

    /** Of the given notifications, the ids this user has read. */
    @Transactional(readOnly = true)
    public Set<UUID> readIdsFor(UUID userId, Collection<UUID> notificationIds) {
        if (notificationIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(readRepository.findReadNotificationIds(userId, notificationIds));
    }

    /**
     * Mark a single notification as read for this user. Returns false if it
     * does not exist or is not visible to them (personal notification of
     * another user, or their own action).
     */
    @Transactional
    public boolean markRead(UUID id, UUID userId) {
        return repository.findById(id)
                .filter(notification -> isVisibleTo(notification, userId))
                .map(notification -> {
                    readRepository.insertReadMark(id, userId);
                    return true;
                })
                .orElse(false);
    }

    /** Mark every notification visible to this user as read. */
    @Transactional
    public int markAllRead(UUID userId) {
        return readRepository.markAllVisibleRead(userId);
    }

    private static boolean isVisibleTo(Notification notification, UUID userId) {
        boolean isRecipient = notification.getRecipientId() == null
                || notification.getRecipientId().equals(userId);
        boolean isOwnAction = userId.equals(notification.getActorId());
        return isRecipient && !isOwnAction;
    }
}
