package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "0")
    })
    @Query("SELECT t FROM Task t WHERE t.status = 'NEW' ORDER BY t.id")
    Optional<Task> findAndLockFirstNewTask();

    Optional<Task> findFirstByStatus(TaskStatus status);
}
