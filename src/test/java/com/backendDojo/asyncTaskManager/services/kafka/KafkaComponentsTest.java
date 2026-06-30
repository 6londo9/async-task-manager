package com.backendDojo.asyncTaskManager.services.kafka;

import com.backendDojo.asyncTaskManager.configs.kafka.KafkaTopicProperties;
import com.backendDojo.asyncTaskManager.exceptions.TaskAlreadyExistsException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.services.notifications.KafkaNotificationsListener;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationSenderService;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import com.backendDojo.asyncTaskManager.services.tasks.KafkaTaskListener;
import com.backendDojo.asyncTaskManager.services.tasks.KafkaTaskSender;
import com.backendDojo.asyncTaskManager.services.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaComponentsTest {

    @Test
    void taskSenderPublishesCreateTaskMessageToConfiguredTopic() {
        KafkaTopicProperties properties = new KafkaTopicProperties();
        properties.setTasks("tasks-topic");
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaTaskSender sender = new KafkaTaskSender(properties, kafkaTemplate);

        sender.publishTask(11L, new TaskRequestDTO("export", 15L));

        ArgumentCaptor<CreateTaskMessage> message = ArgumentCaptor.forClass(CreateTaskMessage.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq("tasks-topic"), message.capture());
        assertEquals("export", message.getValue().name());
        assertEquals(15L, message.getValue().duration());
        assertEquals(11L, message.getValue().userId());
    }

    @Test
    void taskSenderPropagatesKafkaTemplateFailure() {
        KafkaTopicProperties properties = new KafkaTopicProperties();
        properties.setTasks("tasks-topic");
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(String.class), any())).thenThrow(new DataAccessResourceFailureException("kafka down"));
        KafkaTaskSender sender = new KafkaTaskSender(properties, kafkaTemplate);

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> sender.publishTask(11L, new TaskRequestDTO("export", 15L))
        );
    }

    @Test
    void taskListenerSavesConsumedTask() {
        TaskService taskService = mock(TaskService.class);
        KafkaTaskListener listener = new KafkaTaskListener(taskService, mock(NotificationService.class));
        CreateTaskMessage message = new CreateTaskMessage("export", 15L, 11L);

        listener.consume(message);

        verify(taskService).saveTask(message);
    }

    @Test
    void taskListenerCreatesExceptionalNotificationForDuplicateTask() {
        TaskService taskService = mock(TaskService.class);
        NotificationService notificationService = mock(NotificationService.class);
        KafkaTaskListener listener = new KafkaTaskListener(taskService, notificationService);
        CreateTaskMessage message = new CreateTaskMessage("export", 15L, 11L);
        TaskAlreadyExistsException duplicate = new TaskAlreadyExistsException("duplicate");
        when(taskService.saveTask(message)).thenThrow(duplicate);

        listener.consume(message);

        verify(notificationService).saveExceptionalTaskNotification(11L, duplicate);
    }

    @Test
    void notificationListenerDelegatesToSenderService() {
        NotificationSenderService senderService = mock(NotificationSenderService.class);
        KafkaNotificationsListener listener = new KafkaNotificationsListener(senderService);
        NotificationMessage message = new NotificationMessage(44L);

        listener.consume(message);

        verify(senderService).processNotificationFromInbox(message);
    }
}
