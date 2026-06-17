package com.backendDojo.asyncTaskManager.models.dtos.kafka;

import org.apache.commons.lang3.StringUtils;

public record Message(String topic,
                      Object payload) {
    public Message {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Название топика обязательно");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Полезная нагрузка обязательна");
        }
    }
}
