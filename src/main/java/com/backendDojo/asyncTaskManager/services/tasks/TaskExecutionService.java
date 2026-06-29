package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationService;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.ADMIN_ID;

@Service
public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskRepository taskRepository;
    private final ExecutorService executorService;
    private final TaskStatusService taskStatusService;
    private final TaskUpdateService taskUpdateService;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    public TaskExecutionService(TaskRepository taskRepository,
                                ExecutorService executorService,
                                TaskStatusService taskStatusService,
                                TaskUpdateService taskUpdateService,
                                NotificationService notificationService,
                                PlatformTransactionManager transactionManager) {
        this.taskRepository = taskRepository;
        this.executorService = executorService;
        this.taskStatusService = taskStatusService;
        this.taskUpdateService = taskUpdateService;
        this.notificationService = notificationService;
        this.transactionManager = transactionManager;
    }

    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW
    )
    @Retryable(
            value = {
                    OptimisticLockException.class,
                    OptimisticLockingFailureException.class,
                    ObjectOptimisticLockingFailureException.class
            },
            maxRetriesString = "${app.retry-count.tasks}",
            delay = 500
    )
    public void processTaskWithRetry(Task task) {
        try {
            Task currentTask = taskRepository.findById(task.getId())
                    .orElseThrow(() -> new TaskNotFoundException(task.getId()));

            if (currentTask.getStatus() == TaskStatus.IN_PROGRESS || currentTask.getStatus() == TaskStatus.COMPLETED) {
                log.warn("Task with id [{}] already in processing or completed", task.getId());
                return;
            }

            if (currentTask.getStatus() == TaskStatus.FAILED) {
                currentTask.incrementRetryCounter();
            }
            currentTask.setStatus(TaskStatus.IN_PROGRESS);
            currentTask.setStartedAt(OffsetDateTime.now());

            Task savedTask = taskRepository.saveAndFlush(currentTask);

            executorService.submit(() -> executeTaskWithNewConnection(savedTask));
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected for task with id: [{}]", task.getId());
            throw e;
        }
    }

    @Transactional
    public void processNewTaskWithLock() {
        taskRepository.findAndLockFirstNewTask()
                .ifPresent(task -> {
                    log.info("Worker picked task with id: [{}] with SKIP LOCKED", task.getId());
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    task.setStartedAt(OffsetDateTime.now());
                    Task savedTask = taskRepository.save(task);
                    executorService.submit(() ->
                            taskStatusService.executeTask(savedTask));
                });
    }

    @Transactional
    @Retryable(
            value = {
                    OptimisticLockException.class,
                    OptimisticLockingFailureException.class
            },
            maxRetriesString = "${app.retry-count.tasks}",
            delay = 500
    )
    public void failTaskWithNotification(Task task, Exception ex) {
        try {
            taskUpdateService.updateTaskStatus(
                    task.getId(),
                    TaskStatus.FAILED,
                    "Error: " + ex.getMessage()
            );
            notificationService.saveTaskResultNotification(task.getId(), ADMIN_ID);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected for task with id: [{}]", task.getId());
            throw e;
        }
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
                log.info("Task with id: [{}] completed successfully", task.getId());
                notificationService.saveTaskResultNotification(task.getId(), task.getUserId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                taskUpdateService.updateTaskStatus(
                        task.getId(),
                        TaskStatus.FAILED,
                        "Task interrupted: " + e.getMessage()
                );
            } catch (Exception e) {
                log.error("Error executing task with id: [{}]", task.getId(), e);
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
