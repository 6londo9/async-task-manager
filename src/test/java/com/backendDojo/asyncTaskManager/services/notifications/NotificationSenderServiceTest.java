package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.NotificationInboxNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static com.backendDojo.asyncTaskManager.utils.TestFixtures.inbox;
import static com.backendDojo.asyncTaskManager.utils.TestFixtures.notification;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSenderServiceTest {

    @Mock
    private NotificationRepository notificationRepositoryMock;
    @Mock
    private NotificationInboxRepository notificationInboxRepositoryMock;
    @InjectMocks
    private NotificationSenderService notificationSenderService;

    @Test
    void processNotificationCreatesInboxAndMarksItProcessed() {
        Notification notification = notification(1L, 11L, "done");
        NotificationInbox inbox = inbox(notification, false);
        when(notificationInboxRepositoryMock.existsById(notification.getId())).thenReturn(false);
        when(notificationRepositoryMock.findById(notification.getId())).thenReturn(Optional.of(notification));
        doAnswer(invocation -> {
            NotificationInbox savedInbox = invocation.getArgument(0);
            savedInbox.setNotificationId(savedInbox.getNotification().getId());
            return savedInbox;
        }).when(notificationInboxRepositoryMock).save(any(NotificationInbox.class));

        notificationSenderService.processNotificationFromInbox(new NotificationMessage(1L));

        ArgumentCaptor<NotificationInbox> savedInboxCaptor = ArgumentCaptor.forClass(NotificationInbox.class);
        verify(notificationInboxRepositoryMock).save(savedInboxCaptor.capture());
        assertEquals(1L, savedInboxCaptor.getValue().getNotificationId());
        assertNull(savedInboxCaptor.getValue().getStartedAt());
        assertFalse(savedInboxCaptor.getValue().isProcessed());
    }

    @Test
    void processNotificationSkipsWhenMissingNotification() {
        when(notificationRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        notificationSenderService.processNotificationFromInbox(new NotificationMessage(1L));

        verify(notificationInboxRepositoryMock, never()).save(any());
    }

    @Test
    void processNotificationSkipsWhenUnstartedInboxAlreadyExists() {
        when(notificationInboxRepositoryMock.existsById(1L)).thenReturn(true);

        notificationSenderService.processNotificationFromInbox(new NotificationMessage(1L));

        verify(notificationRepositoryMock, never()).findById(any());
        verify(notificationInboxRepositoryMock, never()).save(any());
    }

    @Test
    void processNotificationSkipsConcurrentDuplicateInsert() {
        Notification notification = notification(1L, 11L, "done");
        when(notificationInboxRepositoryMock.existsById(1L)).thenReturn(false);
        when(notificationRepositoryMock.findById(1L)).thenReturn(Optional.of(notification));
        doThrow(new DataIntegrityViolationException("Data already exists"))
                .when(notificationInboxRepositoryMock).save(any(NotificationInbox.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> notificationSenderService.processNotificationFromInbox(new NotificationMessage(1L)));

        verify(notificationInboxRepositoryMock, never()).findById(1L);
    }

    @Test
    void sendNotificationToUserMarksInboxProcessed() {
        Notification notification = notification(1L, 11L, "done");
        NotificationInbox inbox = inbox(notification, false);
        when(notificationInboxRepositoryMock.findById(1L)).thenReturn(Optional.of(inbox));

        notificationSenderService.sendNotificationToUser(inbox);

        assertTrue(inbox.isProcessed());
        verify(notificationInboxRepositoryMock).saveAndFlush(inbox);
        verify(notificationInboxRepositoryMock).save(inbox);
    }

    @Test
    void sendNotificationToUserThrowsWhenInboxDoesNotExist() {
        Notification notification = notification(1L, 11L, "done");
        when(notificationInboxRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotificationInboxNotFoundException.class, () -> notificationSenderService.sendNotificationToUser(inbox(notification, false)));
    }
}
