package com.backendDojo.asyncTaskManager;

import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.services.tasks.TaskExecutionService;
import com.backendDojo.asyncTaskManager.services.tasks.TaskService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// This prevents actual schedulers for triggers, which can ruin test scenarios
@TestPropertySource(properties = {
        "app.scheduler.enabled = false"
})
class TaskConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskExecutionService taskExecutionService;

    @Test
    void sameJvmScheduledWorkerContentionProcessesOneTaskOnce() throws Exception {
        Task task = saveNewTask(uniqueName("retries"), 51L);

        runConcurrently(taskService::processTaskWithRetries);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    Task savedTask = taskRepository.findById(task.getId()).orElseThrow();
                    assertEquals(TaskStatus.COMPLETED, savedTask.getStatus());
                    assertEquals("Task completed successfully", savedTask.getResult());
                    assertEquals(1, completionNotificationsFor(savedTask).size());
                });
    }

    @Test
    void lockedWorkerPathContentionProcessesOneTaskOnce() throws Exception {
        Task task = saveNewTask(uniqueName("locked"), 53L);

        runConcurrently(taskExecutionService::processNewTaskWithLock);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    Task savedTask = taskRepository.findById(task.getId()).orElseThrow();
                    assertEquals(TaskStatus.COMPLETED, savedTask.getStatus());
                    assertEquals("Task completed successfully", savedTask.getResult());
                    assertEquals(1, completionNotificationsFor(savedTask).size());
                });
    }

    private Task saveNewTask(String name, Long userId) {
        Task task = new Task();
        task.setName(name);
        task.setDuration(1L);
        task.setUserId(userId);
        task.setStatus(TaskStatus.NEW);
        return taskRepository.save(task);
    }

    private List<Notification> completionNotificationsFor(Task task) {
        return notificationRepository.findAll()
                .stream()
                .filter(notification -> notification.getUserId().equals(task.getUserId()))
                .filter(notification -> notification.getMessage().contains(task.getName()))
                .filter(notification -> notification.getMessage().contains(TaskStatus.COMPLETED.name()))
                .toList();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
