package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;

@Service
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private final TaskStatusService taskStatusService;

    public TaskExecutionService(TaskRepository taskRepository,
                                ExecutorService executorService,
                                TaskStatusService taskStatusService) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
        this.taskStatusService = taskStatusService;
    }

    @Transactional
    public void processTaskWithRetry(Task task) {
        Task currentTask = taskRepository.findById(task.getId())
                .orElseThrow(() -> new TaskNotFoundException(task.getId()));

        if (currentTask.getStatus() != TaskStatus.NEW && currentTask.getStatus() != TaskStatus.FAILED) {
            log.warn("Task {} already in processing or completed", task.getId());
        }

        currentTask.setStatus(TaskStatus.IN_PROGRESS);

        Task savedTask = taskRepository.save(currentTask);

        executorService.submit(() ->
                taskStatusService.executeTask(savedTask)
        );
    }

    public void processNewTaskWithLock() {
        taskRepository.findAndLockFirstNewTask()
                .ifPresent(task -> {
                    log.info("Worker picked task {} with SKIP LOCKED", task.getId());
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    Task savedTask = taskRepository.save(task);
                    executorService.submit(() ->
                            taskStatusService.executeTask(savedTask));
                });
    }
}
