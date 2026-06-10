package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private final TaskExecutionService taskExecutionService;
    private final KafkaTaskSender kafkaTaskSender;

    public TaskService(TaskRepository taskRepository,
                       ExecutorService executorService,
                       TaskExecutionService taskExecutionService,
                       KafkaTaskSender kafkaTaskSender) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
        this.taskExecutionService = taskExecutionService;
        this.kafkaTaskSender = kafkaTaskSender;
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public void publishTask(TaskRequestDTO taskRequestDTO) {
        kafkaTaskSender.publishTask(taskRequestDTO);
    }

    @Scheduled(fixedDelay = 1000)
    public void processTask() {
        taskExecutionService.processNewTaskWithLock();
    }

    @Scheduled(fixedDelay = 60000)
    public void processFailedTasks() {
        taskRepository.findFirstByStatus(TaskStatus.FAILED)
                .ifPresent(task -> {
                    log.info("Retrying failed task {}", task.getId());
                    taskExecutionService.processTaskWithRetry(task);
                });
    }
}
