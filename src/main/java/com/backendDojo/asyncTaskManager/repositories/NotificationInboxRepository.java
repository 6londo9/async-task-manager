package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    })
    @Query(value = "select i from NotificationInbox i where i.isProcessed = false and i.startedAt < :cutoffTime order by i.notificationId")
    Optional<NotificationInbox> findFirstStalledNotificationWithLock(OffsetDateTime cutoffTime);

    boolean existsByNotificationIdAndStartedAtIsNull(Long notificationId);
}
