package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskAlreadyExistsException;
import com.backendDojo.asyncTaskManager.exceptions.TaskStallException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.ADMIN_ID;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    @Value("${app.retry-count.tasks}")
    private int retryCount;
    @Value("${app.stall-wait-time.tasks}")
    private int stallWaitTime;

    private final TaskRepository taskRepository;
    private final TaskExecutionService taskExecutionService;
    private final KafkaTaskSender kafkaTaskSender;

    public TaskService(TaskRepository taskRepository,
                       TaskExecutionService taskExecutionService,
                       KafkaTaskSender kafkaTaskSender) {
        this.taskRepository = taskRepository;
        this.taskExecutionService = taskExecutionService;
        this.kafkaTaskSender = kafkaTaskSender;
    }

    public Optional<Task> findById(Long id, Long userId) {
        return taskRepository.findById(id).map(task -> {
            if (userId.equals(ADMIN_ID) || userId.equals(task.getUserId())) {
                return task;
            }
            throw new BadCredentialsException("User doesn't have permission to perform this operation");
        });
    }

    public List<Task> findUsersTask(Long userId) {
        return userId.equals(ADMIN_ID)
                ? taskRepository.findAll()
                : taskRepository.findByUserId(userId);
    }

    @Transactional
    public Task saveTask(CreateTaskMessage taskRequestDTO) {
        if (taskRepository.existsByNameAndUserId(taskRequestDTO.name(), taskRequestDTO.userId())) {
            throw new TaskAlreadyExistsException("Task with name: " + taskRequestDTO.name() +
                    " and userId: " + taskRequestDTO.userId() + " already exists");
        }

        Task task = new Task();
        task.setStatus(TaskStatus.NEW);
        task.setName(taskRequestDTO.name());
        task.setDuration(taskRequestDTO.duration());
        task.setUserId(taskRequestDTO.userId());
        return taskRepository.save(task);
    }

    public void publishTask(Long userId, TaskRequestDTO taskRequestDTO) {
        kafkaTaskSender.publishTask(userId, taskRequestDTO);
    }

//    @Scheduled(fixedDelay = 1000)
    public void processTaskWithLock() {
        taskExecutionService.processNewTaskWithLock();
    }

    @Scheduled(fixedDelay = 1_000)
    public void processTaskWithRetries() {
        taskRepository.findFirstByStatus(TaskStatus.NEW)
                .ifPresent(task -> {
                    log.info("Processing task with id: [{}], name: [{}], user: [{}]", task.getId(), task.getName(), task.getUserId());
                    taskExecutionService.processTaskWithRetry(task);
                });
    }

    @Scheduled(fixedDelay = 60_000)
    public void processFailedTasks() {
        taskRepository.findFirstByStatusAndRetryCountLessThan(TaskStatus.FAILED, retryCount)
                .ifPresent(task -> {
                    log.info("Retrying failed task with id: [{}]", task.getId());
                    try {
                        taskExecutionService.processTaskWithRetry(task);
                    } catch (Exception ex) {
                        if (task.getRetryCount() >= retryCount) {
                            taskExecutionService.failTaskWithNotification(task, ex);
                        }
                    }
                });
    }

    @Scheduled(fixedDelay = 90_000)
    public void processStalledTasks() {
        OffsetDateTime now = OffsetDateTime.now();
        taskRepository.findFirstStalledTask(now.minusMinutes(stallWaitTime))
                .ifPresent(task -> {
                    if (task.getRetryCount() >= retryCount) {
                        taskExecutionService.failTaskWithNotification(task, new TaskStallException(retryCount));
                    }

                    log.info("Retrying stalled task with id: [{}], retries: [{}]", task.getId(), task.getRetryCount());
                    taskExecutionService.processTaskWithRetry(task);
                });
    }
}
