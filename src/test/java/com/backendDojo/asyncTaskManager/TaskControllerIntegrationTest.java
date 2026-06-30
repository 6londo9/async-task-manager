package com.backendDojo.asyncTaskManager;

import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.services.notifications.inbox.NotificationInboxService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.ADMIN_ID;
import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.AUTH_HEADER_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NotificationInboxService notificationInboxService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void missingUserHeaderIsRejected() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"missing-user","duration":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankTaskNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header(AUTH_HEADER_NAME, 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","duration":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validTaskRequestIsAccepted() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header(AUTH_HEADER_NAME, 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","duration":1}
                                """.formatted(uniqueName("accepted"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Задача принята в обработку"));
    }

    @Test
    void ownerAdminAndOtherUserReadRulesAreEnforced() throws Exception {
        Task task = new Task();
        task.setName(uniqueName("read"));
        task.setDuration(1L);
        task.setUserId(11L);
        task.setStatus(TaskStatus.COMPLETED);
        task.setResult("done");
        task = taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/{id}", task.getId()).header(AUTH_HEADER_NAME, 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.userId").value(11L));

        mockMvc.perform(get("/api/tasks/{id}", task.getId()).header(AUTH_HEADER_NAME, ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()));

        mockMvc.perform(get("/api/tasks/{id}", task.getId()).header(AUTH_HEADER_NAME, 12L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("permission")));
    }

    @Test
    void adminListsAllTasksAndRegularUserListsOnlyOwnTasks() throws Exception {
        saveTask(uniqueName("admin-one"), 11L);
        saveTask(uniqueName("admin-two"), 12L);

        mockMvc.perform(get("/api/tasks").header(AUTH_HEADER_NAME, ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/tasks").header(AUTH_HEADER_NAME, 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(11L));
    }

    @Test
    void taskRequestCompletesAndCreatesProcessedNotificationInboxThroughDebezium() throws Exception {
        String taskName = uniqueName("full-flow");

        mockMvc.perform(post("/api/tasks")
                        .header(AUTH_HEADER_NAME, 21L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","duration":1}
                                """.formatted(taskName)))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    Task task = findOnlyTaskByName(taskName);
                    assertEquals(TaskStatus.COMPLETED, task.getStatus());
                    assertEquals("Task completed successfully", task.getResult());
                });

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertEquals(1, notifications.size());
                    assertEquals(21L, notifications.get(0).getUserId());
                    assertTrue(notifications.get(0).getMessage().contains(taskName));

                    assertTrue(notificationInboxRepository.findById(notifications.get(0).getId()).isPresent());
                    assertTrue(notificationInboxRepository.findById(notifications.get(0).getId()).orElseThrow().isProcessed());
                });
    }

    @Test
    void duplicateTaskRequestCreatesExceptionalNotification() throws Exception {
        String taskName = uniqueName("duplicate");

        postTask(31L, taskName);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertEquals(1, taskRepository.findByUserId(31L).size()));

        postTask(31L, taskName);

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertTrue(notificationRepository.findAll()
                        .stream()
                        .anyMatch(notification -> notification.getUserId().equals(31L)
                                && notification.getMessage().contains("already exists"))));
    }

    @Test
    void stalledNotificationRetryMarksOldInboxProcessed() {
        Notification notification = new Notification();
        notification.setUserId(41L);
        notification.setMessage("retry me");
        notification = notificationRepository.saveAndFlush(notification);
        Long notificationId = notification.getId();

        NotificationInbox inbox = new NotificationInbox();
        inbox.setNotification(notification);
        inbox.setStartedAt(OffsetDateTime.now().minusMinutes(10));
        inbox.setProcessed(false);
        notificationInboxRepository.saveAndFlush(inbox);
        assertEquals(1, inboxRowCount(notificationId));

        notificationInboxService.processStalledNotifications();

        assertTrue(Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select is_processed from notifications_inbox where notification_id = ?",
                Boolean.class,
                notificationId
        )));
    }

    private void postTask(Long userId, String taskName) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header(AUTH_HEADER_NAME, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","duration":1}
                                """.formatted(taskName)))
                .andExpect(status().isOk());
    }

    private Task saveTask(String name, Long userId) {
        Task task = new Task();
        task.setName(name);
        task.setDuration(1L);
        task.setUserId(userId);
        task.setStatus(TaskStatus.NEW);
        return taskRepository.save(task);
    }

    private Task findOnlyTaskByName(String taskName) {
        return taskRepository.findAll()
                .stream()
                .filter(task -> task.getName().equals(taskName))
                .findFirst()
                .orElseThrow();
    }

    private Integer inboxRowCount(Long notificationId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notifications_inbox where notification_id = ?",
                Integer.class,
                notificationId
        );
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
