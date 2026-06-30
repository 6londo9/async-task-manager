package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(
            value = """
            SELECT *
            FROM tasks t
            WHERE t.status = 'NEW'
            ORDER BY t.id
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<Task> findAndLockFirstNewTask();

    Optional<Task> findFirstByStatus(TaskStatus status);

    Optional<Task> findFirstByStatusAndRetryCountLessThan(TaskStatus status, int retryCount);

    @Query("SELECT t FROM Task t where t.status = 'IN_PROGRESS' AND t.startedAt < :cutoffTime ORDER BY t.id")
    Optional<Task> findFirstStalledTask(OffsetDateTime cutoffTime);

    boolean existsByNameAndUserId(String name, long userId);

    List<Task> findByUserId(Long userId);
}
