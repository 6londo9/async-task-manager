package com.backendDojo.asyncTaskManager.models.dtos.kafka;

import org.apache.commons.lang3.StringUtils;

public record CreateTaskMessage(String name,
                                Long duration,
                                Long userId) {
    public CreateTaskMessage {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Название задачи обязательно");
        }
        if (duration == null) {
            throw new IllegalArgumentException("Длительность задачи обязательна");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя обязателен");
        }
    }
}
