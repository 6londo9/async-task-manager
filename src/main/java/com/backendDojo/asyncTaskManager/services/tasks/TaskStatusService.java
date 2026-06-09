package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import org.springframework.stereotype.Service;

@Service
public class TaskStatusService {

    private final TaskUpdateService taskUpdateService;

    public TaskStatusService(TaskUpdateService taskUpdateService) {
        this.taskUpdateService = taskUpdateService;
    }

    public void executeTask(Task task) {
        try {
            Thread.sleep(task.getDuration());
            taskUpdateService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.COMPLETED,
                    "Task completed successfully"
            );
        } catch (Exception e) {
            taskUpdateService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.FAILED,
                    "Error: " + e.getMessage()
            );
        }
    }
}
