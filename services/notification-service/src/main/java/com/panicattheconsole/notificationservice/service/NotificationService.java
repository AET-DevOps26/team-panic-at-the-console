package com.panicattheconsole.notificationservice.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.panicattheconsole.notificationservice.domain.Notification;
import com.panicattheconsole.notificationservice.domain.NotificationType;
import com.panicattheconsole.notificationservice.messaging.IncidentEvent;
import com.panicattheconsole.notificationservice.repository.NotificationRepository;

import jakarta.transaction.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Turn an incident event into a stored notification. Assignment produces a personal
     * notification for the assigned user; every other subject produces a broadcast.
     * Returns empty for subjects this service does not notify on.
     */
    @Transactional
    public Optional<Notification> record(IncidentEvent event) {

        Notification notification = switch (event.subject()) {

            case "incident.created" -> broadcast(
                    event,
                    NotificationType.INCIDENT_CREATED,
                    "A new incident was opened.");

            case "incident.severity.escalated" -> broadcast(
                    event,
                    NotificationType.SEVERITY_ESCALATED,
                    "Incident severity escalated to " + event.newSeverity() + ".");

            case "incident.resolved" -> broadcast(
                    event,
                    NotificationType.INCIDENT_RESOLVED,
                    "An incident was resolved.");

            case "incident.comment.added" -> broadcast(
                    event,
                    NotificationType.COMMENT_ADDED,
                    "A new comment was added to an incident.");

            case "incident.assigned" -> new Notification(
                    event.incidentId(),
                    NotificationType.INCIDENT_ASSIGNED,
                    event.assignedUserId(),
                    "You were assigned to an incident.",
                    event.timestamp());

            default -> null;
        };

        if (notification == null) {
            log.warn("No notification mapping for subject {}; ignoring", event.subject());
            return Optional.empty();
        }

        Notification saved = repository.save(notification);
        log.debug("Stored {} notification {} for incident {}",
                saved.getType(), saved.getId(), saved.getIncidentId());
        return Optional.of(saved);
    }

    private static Notification broadcast(
            IncidentEvent event,
            NotificationType type,
            String message) {
        return new Notification(
                event.incidentId(),
                type,
                null,
                message,
                event.timestamp());
    }

    public Page<Notification> list(UUID recipientId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (recipientId != null) {
            return unreadOnly
                    ? repository.findVisibleToUnread(recipientId, pageable)
                    : repository.findVisibleTo(recipientId, pageable);
        }
        return unreadOnly
                ? repository.findByReadFalse(pageable)
                : repository.findAll(pageable);
    }

    public long unreadCount(UUID recipientId) {
        return recipientId != null
                ? repository.countVisibleToUnread(recipientId)
                : repository.countByReadFalse();
    }

    /** Mark a single notification as read. Returns false if it does not exist. */
    @Transactional
    public boolean markRead(UUID id) {
        return repository.findById(id)
                .map(notification -> {
                    notification.markRead();
                    return true;
                })
                .orElse(false);
    }

    /** Mark every notification visible to the recipient (or all, when null) as read. */
    @Transactional
    public int markAllRead(UUID recipientId) {
        return repository.markAllReadVisibleTo(recipientId);
    }
}
