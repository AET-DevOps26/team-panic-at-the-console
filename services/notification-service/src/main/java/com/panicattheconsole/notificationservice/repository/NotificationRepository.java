package com.panicattheconsole.notificationservice.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.panicattheconsole.notificationservice.domain.Notification;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    /** Notifications visible to a recipient: their personal ones plus broadcasts. */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientId IS NULL OR n.recipientId = :recipientId
            """)
    Page<Notification> findVisibleTo(
            @Param("recipientId") UUID recipientId,
            Pageable pageable);

    /** Unread subset of {@link #findVisibleTo}. */
    @Query("""
            SELECT n FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :recipientId)
              AND n.read = false
            """)
    Page<Notification> findVisibleToUnread(
            @Param("recipientId") UUID recipientId,
            Pageable pageable);

    /** Unread notifications across all recipients (no recipient filter). */
    Page<Notification> findByReadFalse(Pageable pageable);

    /** Count of unread notifications visible to a recipient. */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :recipientId)
              AND n.read = false
            """)
    long countVisibleToUnread(@Param("recipientId") UUID recipientId);

    /** Count of unread notifications across all recipients. */
    long countByReadFalse();

    /**
     * Mark unread notifications as read. When {@code recipientId} is null every unread
     * notification is marked; otherwise only the recipient's personal ones and broadcasts.
     */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.read = true
            WHERE n.read = false
              AND (:recipientId IS NULL OR n.recipientId IS NULL OR n.recipientId = :recipientId)
            """)
    int markAllReadVisibleTo(@Param("recipientId") UUID recipientId);
}
