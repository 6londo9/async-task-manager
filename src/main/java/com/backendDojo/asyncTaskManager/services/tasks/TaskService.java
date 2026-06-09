package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private final TaskExecutionService taskExecutionService;

    public TaskService(TaskRepository taskRepository,
                       ExecutorService executorService,
                       TaskExecutionService taskExecutionService) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
        this.taskExecutionService = taskExecutionService;
    }

    @Scheduled(fixedDelay = 1000)
    public void processTask() {
        taskRepository.findFirstByStatus(TaskStatus.NEW)
                .ifPresent(taskExecutionService::processTaskWithRetry);
    }
}
