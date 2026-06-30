package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskAlreadyExistsException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.backendDojo.asyncTaskManager.utils.TestFixtures.task;
import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskExecutionService taskExecutionService;
    @Mock
    private KafkaTaskSender kafkaTaskSender;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskExecutionService, kafkaTaskSender);
        ReflectionTestUtils.setField(taskService, "retryCount", 3);
        ReflectionTestUtils.setField(taskService, "stallWaitTime", 15);
    }

    @Test
    void saveTaskPersistsNewTaskWhenNameIsUniqueForUser() {
        CreateTaskMessage message = new CreateTaskMessage("import", 25L, 11L);
        when(taskRepository.existsByNameAndUserId("import", 11L)).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = taskService.saveTask(message);

        assertEquals(TaskStatus.NEW, saved.getStatus());
        assertEquals("import", saved.getName());
        assertEquals(25L, saved.getDuration());
        assertEquals(11L, saved.getUserId());
        verify(taskRepository).save(saved);
    }

    @Test
    void saveTaskRejectsDuplicateNameForSameUser() {
        CreateTaskMessage message = new CreateTaskMessage("import", 25L, 11L);
        when(taskRepository.existsByNameAndUserId("import", 11L)).thenReturn(true);

        assertThrows(TaskAlreadyExistsException.class, () -> taskService.saveTask(message));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsTaskForOwner() {
        Task task = task(1L, 11L, "owned", 1L, TaskStatus.NEW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertSame(task, taskService.findById(1L, 11L).orElseThrow());
    }

    @Test
    void findByIdReturnsTaskForAdmin() {
        Task task = task(1L, 11L, "owned", 1L, TaskStatus.NEW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertSame(task, taskService.findById(1L, ADMIN_ID).orElseThrow());
    }

    @Test
    void findByIdRejectsAnotherUser() {
        Task task = task(1L, 11L, "owned", 1L, TaskStatus.NEW);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(BadCredentialsException.class, () -> taskService.findById(1L, 12L));
    }

    @Test
    void findUsersTaskReturnsAllForAdmin() {
        List<Task> tasks = List.of(task(1L, 11L, "one", 1L, TaskStatus.NEW));
        when(taskRepository.findAll()).thenReturn(tasks);

        assertSame(tasks, taskService.findUsersTask(ADMIN_ID));
    }

    @Test
    void findUsersTaskReturnsOnlyUserTasksForRegularUser() {
        List<Task> tasks = List.of(task(1L, 11L, "one", 1L, TaskStatus.NEW));
        when(taskRepository.findByUserId(11L)).thenReturn(tasks);

        assertSame(tasks, taskService.findUsersTask(11L));
    }

    @Test
    void publishTaskDelegatesToKafkaSender() {
        TaskRequestDTO request = new TaskRequestDTO("import", 10L);

        taskService.publishTask(11L, request);

        verify(kafkaTaskSender).publishTask(11L, request);
    }

    @Test
    void processTaskDelegatesToLockedExecutionPath() {
        taskService.processTaskWithLock();

        verify(taskExecutionService).processNewTaskWithLock();
    }

    @Test
    void processFailedTasksFailsWithNotificationWhenRetryLimitIsReachedAfterRetryException() {
        Task task = task(1L, 11L, "failed", 1L, TaskStatus.FAILED);
        task.incrementRetryCounter();
        task.incrementRetryCounter();
        task.incrementRetryCounter();
        RuntimeException retryError = new RuntimeException("still broken");
        when(taskRepository.findFirstByStatusAndRetryCountLessThan(TaskStatus.FAILED, 3)).thenReturn(Optional.of(task));
        org.mockito.Mockito.doThrow(retryError).when(taskExecutionService).processTaskWithRetry(task);

        taskService.processFailedTasks();

        verify(taskExecutionService).failTaskWithNotification(task, retryError);
    }

    @Test
    void processStalledTasksFailsAndRetriesWhenRetryLimitIsReached() {
        Task task = task(1L, 11L, "stalled", 1L, TaskStatus.IN_PROGRESS);
        task.incrementRetryCounter();
        task.incrementRetryCounter();
        task.incrementRetryCounter();
        when(taskRepository.findFirstStalledTask(any(OffsetDateTime.class))).thenReturn(Optional.of(task));

        taskService.processStalledTasks();

        ArgumentCaptor<Exception> exception = ArgumentCaptor.forClass(Exception.class);
        verify(taskExecutionService).failTaskWithNotification(eq(task), exception.capture());
        assertEquals("Task exceeded maximum retry count: 3", exception.getValue().getMessage());
        verify(taskExecutionService).processTaskWithRetry(task);
    }
}
