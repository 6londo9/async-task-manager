package com.backendDojo.asyncTaskManager.repositories;

import com.backendDojo.asyncTaskManager.models.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
