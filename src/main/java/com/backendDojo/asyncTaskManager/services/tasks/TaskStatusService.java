package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskStatusService {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusService.class);

    private final TaskUpdateService taskUpdateService;
    private final NotificationService notificationService;

    public TaskStatusService(TaskUpdateService taskUpdateService, NotificationService notificationService) {
        this.taskUpdateService = taskUpdateService;
        this.notificationService = notificationService;
    }

    public void executeTask(Task task) {
        try {
            Thread.sleep(task.getDuration());
            taskUpdateService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.COMPLETED,
                    "Task completed successfully"
            );
            log.info("Task with id: [{}] completed successfully", task.getId());
            notificationService.saveTaskResultNotification(task.getId(), task.getUserId());
        } catch (Exception e) {
            taskUpdateService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.FAILED,
                    "Error: " + e.getMessage()
            );
        }
    }
}
