package com.panicattheconsole.notificationservice.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.panicattheconsole.notificationservice.domain.Notification;

/**
 * Visibility rule shared by every query: a user sees their personal
 * notifications plus broadcasts, but never notifications produced by their
 * own actions. Read state is per user via {@code NotificationRead} marks.
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    @Query("""
            SELECT n FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND (n.actorId IS NULL OR n.actorId <> :userId)
            """)
    Page<Notification> findVisibleTo(
            @Param("userId") UUID userId,
            Pageable pageable);

    /** Unread subset of {@link #findVisibleTo}: no read mark by this user. */
    @Query("""
            SELECT n FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND (n.actorId IS NULL OR n.actorId <> :userId)
              AND NOT EXISTS (
                  SELECT 1 FROM NotificationRead r
                  WHERE r.id.notificationId = n.id AND r.id.userId = :userId)
            """)
    Page<Notification> findVisibleToUnread(
            @Param("userId") UUID userId,
            Pageable pageable);

    /** Count of unread notifications visible to a user. */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE (n.recipientId IS NULL OR n.recipientId = :userId)
              AND (n.actorId IS NULL OR n.actorId <> :userId)
              AND NOT EXISTS (
                  SELECT 1 FROM NotificationRead r
                  WHERE r.id.notificationId = n.id AND r.id.userId = :userId)
            """)
    long countVisibleToUnread(@Param("userId") UUID userId);
}
