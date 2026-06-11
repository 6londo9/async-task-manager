package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private final TaskStatusService taskStatusService;
    private final TaskUpdateService taskUpdateService;
    private final PlatformTransactionManager transactionManager;

    public TaskExecutionService(TaskRepository taskRepository,
                                ExecutorService executorService,
                                TaskStatusService taskStatusService,
                                TaskUpdateService taskUpdateService,
                                PlatformTransactionManager transactionManager) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
        this.taskStatusService = taskStatusService;
        this.taskUpdateService = taskUpdateService;
        this.transactionManager = transactionManager;
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            value = OptimisticLockException.class,
            maxRetries = 3,
            delay = 500,
            multiplier = 2
    )
    public void processTaskWithRetry(Task task) {
        try {
            Task currentTask = taskRepository.findById(task.getId())
                    .orElseThrow(() -> new TaskNotFoundException(task.getId()));

            if (currentTask.getStatus() != TaskStatus.NEW) {
                log.warn("Task {} already in processing or completed", task.getId());
                return;
            }

            currentTask.setStatus(TaskStatus.IN_PROGRESS);

            Task savedTask = taskRepository.save(currentTask);

            executorService.submit(() -> executeTaskWithNewConnection(savedTask));
        } catch (OptimisticLockException e) {
            log.warn("Concurrent modification detected for task {}", task.getId());
            throw e;
        }
    }

    @Transactional
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

    private void executeTaskWithNewConnection(Task task) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.execute(status -> {
            try {
                Thread.sleep(task.getDuration());
                taskUpdateService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.COMPLETED,
                        "Task completed successfully"
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                taskUpdateService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.FAILED,
                        "Task interrupted: " + e.getMessage()
                );
            } catch (Exception e) {
                log.error("Error executing task {}", task.getId(), e);
                taskUpdateService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.FAILED,
                        "Error: " + e.getMessage()
                );
            }
            return null;
        });
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
