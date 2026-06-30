package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String FORMATTED_MESSAGE = "%s, id: [%s], name: [%s], status: [%s]";
    private static final String FORMATTED_EXCEPTIONAL_MESSAGE = "Task processing failed with an exception: [%s]";

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;

    public NotificationService(NotificationRepository notificationRepository, TaskRepository taskRepository) {
        this.notificationRepository = notificationRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void saveTaskResultNotification(Long taskId, Long userId) {
        Task savedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        String message = FORMATTED_MESSAGE.formatted(savedTask.getResult(), savedTask.getId(), savedTask.getName(), savedTask.getStatus());
        this.saveNotificationInternal(userId, message);
    }

    @Transactional
    public void saveExceptionalTaskNotification(Long userId, Exception ex) {
        String message = FORMATTED_EXCEPTIONAL_MESSAGE.formatted(ex.getMessage());
        this.saveNotificationInternal(userId, message);
    }

    private void saveNotificationInternal(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(StringUtils.abbreviate(message, 300));

        notification = notificationRepository.save(notification);
        log.info("Notification with id: [{}] saved successfully for userId: [{}] with message: [{}]", notification.getId(), userId, message);
    }
}
