package com.backendDojo.asyncTaskManager;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationSenderService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationSenderService notificationSenderService;

    @Test
    void concurrentNotificationProcessingCreatesOneProcessedInbox() throws Exception {
        Notification notification = new Notification();
        notification.setUserId(54L);
        notification.setMessage("concurrent notification");
        notification = notificationRepository.save(notification);
        NotificationMessage message = new NotificationMessage(notification.getId());

        runConcurrently(() -> notificationSenderService.processNotificationFromInbox(message));

        Long notificationId = notification.getId();
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertEquals(1, notificationInboxRepository.count());
                    assertTrue(notificationInboxRepository.findById(notificationId).orElseThrow().isProcessed());
                });
    }
}
