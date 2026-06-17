package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.models.entities.keys.NotificationMappingKey;
import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class NotificationSenderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository notificationInboxRepository;

    @Value("${app.stall-wait-time.notifications}")
    private Long stallWaitTime;

    public NotificationSenderService(NotificationRepository notificationRepository, NotificationInboxRepository notificationInboxRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationInboxRepository = notificationInboxRepository;
    }

    @Transactional
    public void processNotificationFromInbox(NotificationMessage notificationMessage) {
        NotificationMappingKey id = new NotificationMappingKey();
        id.setTaskId(notificationMessage.taskId());
        id.setUserId(notificationMessage.userId());
        Optional<NotificationInbox> inboxOpt = notificationInboxRepository.findByIdWithLock(id);
        OffsetDateTime now = OffsetDateTime.now();

        NotificationInbox inbox;
        if (inboxOpt.isPresent()) {
            inbox = inboxOpt.get();
            if (inbox.getStartedAt() != null && inbox.getStartedAt().plusMinutes(stallWaitTime).isBefore(now)) {
                return;
            }

            inbox.setStartedAt(now);
        } else {
            inbox = new NotificationInbox();
            inbox.setStartedAt(now);
            Optional<Notification> notificationOpt = notificationRepository.findById(id);
            if (notificationOpt.isEmpty()) {
                log.error("No notification found with id {}", id);
                return;
            }
            inbox.setNotification(notificationOpt.get());
        }
        notificationInboxRepository.save(inbox);

        log.info("Sending notification message to the user: [{}] about task: [{}] with status: [{}]",
                inbox.getNotification().getTask().getUserId(),
                inbox.getNotification().getTask().getName(),
                inbox.getNotification().getTask().getStatus());

        inbox.setProcessed(true);
        notificationInboxRepository.save(inbox);
    }
}
