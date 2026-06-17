package com.backendDojo.asyncTaskManager.models.dtos.kafka;

public record NotificationMessage(Long taskId,
                                  Long userId) {
    public NotificationMessage {
        if (taskId == null) {
            throw new IllegalArgumentException("Идентификатор задачи обязателен");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя обязателен");
        }
    }
}
