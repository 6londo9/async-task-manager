package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static com.backendDojo.asyncTaskManager.utils.TestFixtures.completedTask;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void saveTaskResultNotificationStoresMessageFromTaskState() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        NotificationService service = new NotificationService(notificationRepository, taskRepository);
        Task task = completedTask(1L, 11L, "export");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveTaskResultNotification(1L, 11L);

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertEquals(11L, notification.getValue().getUserId());
        assertEquals("Task completed successfully, id: [1], name: [export], status: [COMPLETED]", notification.getValue().getMessage());
    }

    @Test
    void saveTaskResultNotificationThrowsWhenTaskDoesNotExist() {
        NotificationService service = new NotificationService(mock(NotificationRepository.class), mock(TaskRepository.class));

        assertThrows(TaskNotFoundException.class, () -> service.saveTaskResultNotification(404L, 11L));
    }

    @Test
    void saveExceptionalTaskNotificationStoresExceptionMessage() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(notificationRepository, mock(TaskRepository.class));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveExceptionalTaskNotification(11L, new RuntimeException("duplicate"));

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertEquals(11L, notification.getValue().getUserId());
        assertEquals("Task processing failed with an exception: [duplicate]", notification.getValue().getMessage());
    }

    @Test
    void saveExceptionalTaskNotificationPropagatesRepositoryFailure() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(notificationRepository, mock(TaskRepository.class));
        when(notificationRepository.save(any(Notification.class))).thenThrow(new DataAccessResourceFailureException("database down"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.saveExceptionalTaskNotification(11L, new RuntimeException("duplicate"))
        );
    }
}
