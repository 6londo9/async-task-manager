package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    @Query(
            value = """
            SELECT *
            FROM notifications_inbox i
            WHERE i.is_processed = false
              AND i.started_at IS NULL
            ORDER BY i.notification_id
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<NotificationInbox> findFirstNonProcessedNotificationWithLock();

    @Query(
            value = """
            SELECT *
            FROM notifications_inbox i
            WHERE i.is_processed = false
              AND i.started_at < :cutoffTime
            ORDER BY i.notification_id
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<NotificationInbox> findFirstStalledNotificationWithLock(OffsetDateTime cutoffTime);

    @Modifying
    @Query(
            value = """
            DELETE
            FROM NotificationInbox
            WHERE isProcessed = true
              AND startedAt < :cutoffTime
            """
    )
    void deleteAllProcessedByStartedAtBefore(OffsetDateTime cutoffTime);
}
