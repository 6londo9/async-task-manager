package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.models.entities.keys.NotificationMappingKey;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, NotificationMappingKey> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    })
    @Query(value = "from NotificationInbox i where i.id = :id")
    Optional<NotificationInbox> findByIdWithLock(NotificationMappingKey id);
}
