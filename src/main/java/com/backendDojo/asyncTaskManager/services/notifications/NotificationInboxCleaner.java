package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class NotificationInboxCleaner {

    private final NotificationInboxRepository notificationInboxRepository;

    @Value("${app.schedulers.inbox.cleanup-for-time:2}")
    private int scheduledCleanupForTime;

    public NotificationInboxCleaner(NotificationInboxRepository notificationInboxRepository) {
        this.notificationInboxRepository = notificationInboxRepository;
    }

    @Scheduled(cron = "${app.schedulers.inbox.cleanup-cron-time:0 0 0 1 */2 *}")
    @SchedulerLock(
            name = "NotificationInboxScheduler_cleanInbox",
            lockAtLeastFor = "PT3M",
            lockAtMostFor = "PT15M"
    )
    @Transactional
    public void cleanInbox() {
        OffsetDateTime now = OffsetDateTime.now();
        notificationInboxRepository.deleteAllProcessedByStartedAtBefore(now.minusMonths(scheduledCleanupForTime));
    }
}
