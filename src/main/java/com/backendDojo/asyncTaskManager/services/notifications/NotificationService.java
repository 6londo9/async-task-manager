package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.entities.keys.NotificationMappingKey;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final long ADMIN_ID = 0;

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;

    public NotificationService(NotificationRepository notificationRepository, TaskRepository taskRepository) {
        this.notificationRepository = notificationRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void saveNotificationForTask(Long taskId, boolean isErrorNotification) {
        Task savedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Notification notification = new Notification();
        NotificationMappingKey key = new NotificationMappingKey();
        key.setTaskId(savedTask.getId());
        key.setUserId(isErrorNotification ? ADMIN_ID : savedTask.getUserId());
        notification.setId(key);
        notification.setTask(savedTask);

        notificationRepository.save(notification);
    }
}
