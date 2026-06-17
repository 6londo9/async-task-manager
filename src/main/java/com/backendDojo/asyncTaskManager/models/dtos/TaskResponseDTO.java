package com.backendDojo.asyncTaskManager.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

@Schema(description = "Информация о задаче")
public record TaskResponseDTO(@Schema(description = "Идентификатор задачи", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "9223372036854775807", example = "23")
                              Long id,
                              @Schema(description = "Название задачи", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, example = "Clean room")
                              String name,
                              @Schema(description = "Статус задачи", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, example = "COMPLETED")
                              String status,
                              @Schema(description = "Результат выполнения задачи", requiredMode = Schema.RequiredMode.NOT_REQUIRED, minLength = 1, example = "Task completed successfully")
                              String result) {
    public TaskResponseDTO {
        if (id == null) {
            throw new IllegalArgumentException("Идентификатор задачи обязателен");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Название задачи обязательно");
        }
        if (StringUtils.isBlank(status)) {
            throw new IllegalArgumentException("Статус задачи обязателен");
        }
    }
}
