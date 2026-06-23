package com.backendDojo.asyncTaskManager.models.dtos.kafka;

public record NotificationMessage(Long notificationId) {
    public NotificationMessage {
        if (notificationId == null) {
            throw new IllegalArgumentException("Идентификатор оповещения обязателен");
        }
    }
}
