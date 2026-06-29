package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.backendDojo.asyncTaskManager.utils.TestFixtures.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskUpdateAndStatusServiceTest {

    @Mock
    private TaskRepository taskRepositoryMock;
    @Mock
    private TaskUpdateService taskUpdateServiceMock;
    @Mock
    private NotificationService notificationServiceMock;
    @InjectMocks
    private TaskUpdateService taskUpdateService;
    @InjectMocks
    private TaskStatusService taskStatusService;

    @Test
    void updateTaskStatusPersistsNewStatusAndResult() {
        Task task = task(1L, 11L, "task", 1L, TaskStatus.NEW);
        when(taskRepositoryMock.findById(1L)).thenReturn(Optional.of(task));

        taskUpdateService.updateTaskStatus(1L, TaskStatus.COMPLETED, "done");

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals("done", task.getResult());
        verify(taskRepositoryMock).save(task);
    }

    @Test
    void updateTaskStatusThrowsWhenTaskDoesNotExist() {
        when(taskRepositoryMock.findById(404L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskUpdateService.updateTaskStatus(404L, TaskStatus.FAILED, "missing"));
    }

    @Test
    void executeTaskMarksTaskCompleted() {
        Task task = task(1L, 11L, "task", 1L, TaskStatus.IN_PROGRESS);

        taskStatusService.executeTask(task);

        verify(taskUpdateServiceMock).updateTaskStatus(1L, TaskStatus.COMPLETED, "Task completed successfully");
    }

    @Test
    void executeTaskMarksTaskFailedWhenSleepIsInterrupted() {
        Task task = task(1L, 11L, "task", Long.MAX_VALUE, TaskStatus.IN_PROGRESS);

        Thread.currentThread().interrupt();
        try {
            taskStatusService.executeTask(task);
        } finally {
            Thread.interrupted();
        }

        verify(taskUpdateServiceMock).updateTaskStatus(1L, TaskStatus.FAILED, "Error: sleep interrupted");
    }
}
