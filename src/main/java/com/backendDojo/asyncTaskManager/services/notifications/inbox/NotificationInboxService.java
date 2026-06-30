package com.backendDojo.asyncTaskManager.services.notifications.inbox;

import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.services.notifications.NotificationSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class NotificationInboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInboxService.class);

    private final NotificationInboxRepository notificationInboxRepository;
    private final NotificationSenderService notificationSenderService;

    @Value("${app.stall-wait-time.notifications}")
    private Long stallWaitTime;

    public NotificationInboxService(NotificationInboxRepository notificationInboxRepository, NotificationSenderService notificationSenderService) {
        this.notificationInboxRepository = notificationInboxRepository;
        this.notificationSenderService = notificationSenderService;
    }

    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void processTask() {
        notificationInboxRepository.findFirstNonProcessedNotificationWithLock()
                .ifPresent(notificationInbox -> {
                    log.info("Trying to send notification with id: [{}]", notificationInbox.getNotificationId());
                    notificationSenderService.sendNotificationToUser(notificationInbox);
                });
    }

    @Scheduled(fixedRate = 90_000)
    @Transactional
    public void processStalledNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        notificationInboxRepository.findFirstStalledNotificationWithLock(now.minusMinutes(stallWaitTime))
                .ifPresent(notificationInbox -> {
                    log.info("Retrying to send stalled notification with id: [{}]", notificationInbox.getNotificationId());
                    notificationSenderService.sendNotificationToUser(notificationInbox);
                });
    }
}
