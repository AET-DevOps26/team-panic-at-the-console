package com.panicattheconsole.notificationservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.panicattheconsole.notificationservice.domain.NotificationRead;
import com.panicattheconsole.notificationservice.domain.NotificationReadId;

/**
 * Per-user read marks. Inserts use native Postgres {@code ON CONFLICT DO
 * NOTHING} so marking is idempotent without a read-then-write race (the
 * service runs on Postgres in production and in the Testcontainers tests).
 */
@Repository
public interface NotificationReadRepository
        extends JpaRepository<NotificationRead, NotificationReadId> {

    /** Of the given notifications, the ids this user has already read. */
    @Query("""
            SELECT r.id.notificationId FROM NotificationRead r
            WHERE r.id.userId = :userId AND r.id.notificationId IN :ids
            """)
    List<UUID> findReadNotificationIds(
            @Param("userId") UUID userId,
            @Param("ids") Collection<UUID> ids);

    @Modifying
    @Query(value = """
            INSERT INTO notification_reads (notification_id, user_id, read_at)
            VALUES (:notificationId, :userId, now())
            ON CONFLICT (notification_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertReadMark(
            @Param("notificationId") UUID notificationId,
            @Param("userId") UUID userId);

    /** Mark every notification visible to the user (same rule as NotificationRepository) as read. */
    @Modifying
    @Query(value = """
            INSERT INTO notification_reads (notification_id, user_id, read_at)
            SELECT n.id, :userId, now() FROM notifications n
            WHERE (n.recipient_id IS NULL OR n.recipient_id = :userId)
              AND (n.actor_id IS NULL OR n.actor_id <> :userId)
            ON CONFLICT (notification_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int markAllVisibleRead(@Param("userId") UUID userId);
}
