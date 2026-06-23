package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskAlreadyExistsException;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaTaskListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaTaskListener.class);

    private final TaskService taskService;
    private final NotificationService notificationService;

    public KafkaTaskListener(TaskService taskService, NotificationService notificationService) {
        this.taskService = taskService;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            groupId = "${app.kafka.tasks.consumers.group-id}",
            topics = {"${app.kafka.topics.tasks}"},
            containerFactory = "kafkaTaskListenerContainerFactory"
    )
    public void consume(CreateTaskMessage createTaskMessage) {
        log.info("Received task request: [{}]", createTaskMessage);
        try {
            taskService.saveTask(createTaskMessage);
        } catch (TaskAlreadyExistsException ex) {
            log.info("Task with name: [{}], which made by user: [{}] already exists. Sending this info to the client", createTaskMessage.name(), createTaskMessage.userId());
            notificationService.saveExceptionalTaskNotification(createTaskMessage.userId(), ex);
        }
    }
}
