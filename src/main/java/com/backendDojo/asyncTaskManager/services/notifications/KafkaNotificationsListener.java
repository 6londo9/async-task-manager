package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaNotificationsListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationsListener.class);

    private final NotificationSenderService notificationSenderService;

    public KafkaNotificationsListener(NotificationSenderService notificationSenderService) {
        this.notificationSenderService = notificationSenderService;
    }

    @KafkaListener(
            groupId = "${app.kafka.notifications.consumers.group-id}",
            topics = {"${app.kafka.topics.notifications}"},
            containerFactory = "kafkaNotificationListenerContainerFactory"
    )
    public void consume(NotificationMessage notificationMessage) {
        log.info("Got notification message with id: [{}]. Preparing to send it to the user", notificationMessage.notificationId());
        notificationSenderService.processNotificationFromInbox(notificationMessage);
    }
}
