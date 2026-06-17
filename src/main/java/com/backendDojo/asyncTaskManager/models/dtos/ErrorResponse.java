package com.backendDojo.asyncTaskManager.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

@Schema(description = "Ответ с информацией об ошибке")
public record ErrorResponse(@Schema(description = "Название задачи", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, example = "Task not found")
                            String message) {
    public ErrorResponse {
        if (StringUtils.isBlank(message)) {
            throw new IllegalArgumentException("Ошибочный ответ должен содержать сообщение об ошибке");
        }
    }
}
