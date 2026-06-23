package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.NotificationInboxNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class NotificationSenderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository notificationInboxRepository;

    public NotificationSenderService(NotificationRepository notificationRepository, NotificationInboxRepository notificationInboxRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationInboxRepository = notificationInboxRepository;
    }

    @Transactional
    public void processNotificationFromInbox(NotificationMessage notificationMessage) {
        if (notificationInboxRepository.existsByNotificationIdAndStartedAtIsNull(notificationMessage.notificationId())) {
            log.warn("Already saved inbox notification with id: [{}]. Skipping", notificationMessage.notificationId());
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationMessage.notificationId());
        if (notificationOpt.isEmpty()) {
            log.error("No notification found with id: [{}]", notificationMessage.notificationId());
            return;
        }
        NotificationInbox inbox = new NotificationInbox();
        inbox.setNotification(notificationOpt.get());
        inbox.setStartedAt(now);
        try {
            notificationInboxRepository.save(inbox);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent saving of inbox notification with id: [{}]. Skipping", notificationMessage.notificationId(), ex);
            return;
        }

        this.sendNotificationToUser(inbox);
    }

    @Transactional
    public void sendNotificationToUser(NotificationInbox notificationInbox) {
        NotificationInbox savedInbox = notificationInboxRepository.findById(notificationInbox.getNotificationId())
                .orElseThrow(() -> new NotificationInboxNotFoundException(notificationInbox.getNotificationId()));

        log.info("Sending notification with id: [{}], message: [{}]", savedInbox.getNotificationId(), savedInbox.getNotification().getMessage());

        savedInbox.setProcessed(true);
        notificationInboxRepository.save(savedInbox);
    }
}
