package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.backendDojo.asyncTaskManager.utils.TestFixtures.task;
import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskExecutionServiceTest {

    private TaskRepository taskRepository;
    private ExecutorService executorService;
    private TaskStatusService taskStatusService;
    private TaskUpdateService taskUpdateService;
    private NotificationService notificationService;
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        executorService = mock(ExecutorService.class);
        taskStatusService = mock(TaskStatusService.class);
        taskUpdateService = mock(TaskUpdateService.class);
        notificationService = mock(NotificationService.class);
        taskExecutionService = new TaskExecutionService(
                taskRepository,
                executorService,
                taskStatusService,
                taskUpdateService,
                notificationService,
                mock(PlatformTransactionManager.class)
        );
    }

    @Test
    void processTaskWithRetryMovesNewTaskToInProgressAndSubmitsExecution() {
        Task task = task(1L, 11L, "new", 1L, TaskStatus.NEW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        taskExecutionService.processTaskWithRetry(task);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertNotNull(task.getStartedAt());
        verify(taskRepository).saveAndFlush(task);
        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    void processTaskWithRetrySkipsInProgressTask() {
        Task task = task(1L, 11L, "running", 1L, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskExecutionService.processTaskWithRetry(task);

        verify(taskRepository, never()).save(any());
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void processTaskWithRetrySkipsCompletedTask() {
        Task task = task(1L, 11L, "done", 1L, TaskStatus.COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskExecutionService.processTaskWithRetry(task);

        verify(taskRepository, never()).save(any());
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void processTaskWithRetryIncrementsFailedTaskRetryCountBeforeSubmitting() {
        Task task = task(1L, 11L, "failed", 1L, TaskStatus.FAILED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskExecutionService.processTaskWithRetry(task);

        assertEquals(1, task.getRetryCount());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    void processTaskWithRetryThrowsWhenTaskDoesNotExist() {
        Task task = task(404L, 11L, "missing", 1L, TaskStatus.NEW);
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskExecutionService.processTaskWithRetry(task));
    }

    @Test
    void processTaskWithRetryRethrowsOptimisticLockException() {
        Task task = task(1L, 11L, "new", 1L, TaskStatus.NEW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenThrow(new OptimisticLockException("conflict"));

        assertThrows(OptimisticLockException.class, () -> taskExecutionService.processTaskWithRetry(task));

        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void processNewTaskWithLockUsesLockedRepositoryQuery() {
        Task task = task(1L, 11L, "new", 1L, TaskStatus.NEW);
        when(taskRepository.findAndLockFirstNewTask()).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskExecutionService.processNewTaskWithLock();

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        verify(taskRepository).save(task);
        verify(executorService).submit(any(Runnable.class));
    }

    @Test
    void failTaskWithNotificationMarksTaskFailedAndNotifiesAdmin() {
        Task task = task(1L, 11L, "failed", 1L, TaskStatus.IN_PROGRESS);
        RuntimeException error = new RuntimeException("boom");

        taskExecutionService.failTaskWithNotification(task, error);

        verify(taskUpdateService).updateTaskStatus(1L, TaskStatus.FAILED, "Error: boom");
        verify(notificationService).saveTaskResultNotification(1L, ADMIN_ID);
    }

    @Test
    void failTaskWithNotificationRethrowsOptimisticLockException() {
        Task task = task(1L, 11L, "failed", 1L, TaskStatus.IN_PROGRESS);
        OptimisticLockException conflict = new OptimisticLockException("conflict");
        org.mockito.Mockito.doThrow(conflict)
                .when(taskUpdateService)
                .updateTaskStatus(1L, TaskStatus.FAILED, "Error: boom");

        assertThrows(
                OptimisticLockException.class,
                () -> taskExecutionService.failTaskWithNotification(task, new RuntimeException("boom"))
        );

        verify(notificationService, never()).saveTaskResultNotification(any(), any());
    }
}
