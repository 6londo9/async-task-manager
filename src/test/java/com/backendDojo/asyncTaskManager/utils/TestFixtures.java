package com.backendDojo.asyncTaskManager.utils;

import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

public final class TestFixtures {

    public static Task task(Long id, Long userId, String name, Long duration, TaskStatus status) {
        Task task = new Task();
        ReflectionTestUtils.setField(task, "id", id);
        task.setUserId(userId);
        task.setName(name);
        task.setDuration(duration);
        task.setStatus(status);
        return task;
    }

    public static Task completedTask(Long id, Long userId, String name) {
        Task task = task(id, userId, name, 1L, TaskStatus.COMPLETED);
        task.setResult("Task completed successfully");
        task.setStartedAt(OffsetDateTime.now());
        return task;
    }

    public static Notification notification(Long id, Long userId, String message) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setMessage(message);
        return notification;
    }

    public static NotificationInbox inbox(Notification notification, boolean processed) {
        NotificationInbox inbox = new NotificationInbox();
        inbox.setNotification(notification);
        inbox.setNotificationId(notification.getId());
        inbox.setStartedAt(OffsetDateTime.now());
        inbox.setProcessed(processed);
        return inbox;
    }
}
