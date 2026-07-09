package com.backendDojo.asyncTaskManager.services.notifications;

import com.backendDojo.asyncTaskManager.exceptions.NotificationInboxNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import com.backendDojo.asyncTaskManager.models.entities.NotificationInbox;
import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationSenderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationInboxRepository notificationInboxRepository;
    private final RedisTemplate<Long, NotificationInbox> notificationInboxRedisTemplate;

    @Value("${app.redis.notification-inbox.ttl:60}")
    private int inboxCacheTtl;

    public NotificationSenderService(NotificationRepository notificationRepository,
                                     NotificationInboxRepository notificationInboxRepository,
                                     RedisTemplate<Long, NotificationInbox> notificationInboxRedisTemplate) {
        this.notificationRepository = notificationRepository;
        this.notificationInboxRepository = notificationInboxRepository;
        this.notificationInboxRedisTemplate = notificationInboxRedisTemplate;
    }

    @Transactional
    @Retryable(
            value = {
                    DataIntegrityViolationException.class
            },
            maxRetriesString = "${app.retry-count.notification-inbox:3}",
            delay = 500
    )
    public void processNotificationFromInbox(NotificationMessage notificationMessage) {
        NotificationInbox cachedInbox = null;
        try {
            cachedInbox = notificationInboxRedisTemplate.opsForValue().get(notificationMessage.notificationId());
        } catch (Exception ex) {
            // Simply ignore redis exception to not ruin our main logic
            log.warn("Something went wrong while getting notification inbox from redis cache, notificationId: [{}]",
                    notificationMessage.notificationId(), ex);
        }
        if (cachedInbox != null) {
            log.warn("Already saved inbox notification in cache with id: [{}]. Skipping", notificationMessage.notificationId());
            return;
        }

        if (notificationInboxRepository.existsById(notificationMessage.notificationId())) {
            log.warn("Already saved inbox notification with id: [{}]. Skipping", notificationMessage.notificationId());
            return;
        }

        Optional<Notification> notificationOpt = notificationRepository.findById(notificationMessage.notificationId());
        if (notificationOpt.isEmpty()) {
            log.error("No notification found with id: [{}]", notificationMessage.notificationId());
            return;
        }
        NotificationInbox inbox = new NotificationInbox();
        inbox.setNotification(notificationOpt.get());
        try {
            notificationInboxRepository.save(inbox);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent saving of inbox notification with id: [{}]. Skipping", notificationMessage.notificationId());
            throw ex;
        }
        try {
            notificationInboxRedisTemplate.opsForValue().set(notificationMessage.notificationId(), inbox, inboxCacheTtl, TimeUnit.SECONDS);
        } catch (Exception ex) {
            // Simply ignore redis exception to not ruin our main logic
            log.warn("Something went wrong while saving notification inbox from redis cache, notificationId: [{}]",
                    notificationMessage.notificationId(), ex);
        }
    }

    @Transactional
    public void sendNotificationToUser(NotificationInbox notificationInbox) {
        NotificationInbox savedInbox = notificationInboxRepository.findById(notificationInbox.getNotificationId())
                .orElseThrow(() -> new NotificationInboxNotFoundException(notificationInbox.getNotificationId()));
        savedInbox.setStartedAt(OffsetDateTime.now());
        notificationInboxRepository.saveAndFlush(savedInbox);

        log.info("Sending notification with id: [{}], message: [{}]", savedInbox.getNotificationId(), savedInbox.getNotification().getMessage());

        savedInbox.setProcessed(true);
        notificationInboxRepository.save(savedInbox);
    }
}
