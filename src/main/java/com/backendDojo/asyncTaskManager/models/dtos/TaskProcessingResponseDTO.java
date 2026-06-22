package com.backendDojo.asyncTaskManager.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

@Schema(description = "Информация о задаче")
public record TaskProcessingResponseDTO(@Schema(description = "Сообщение о результате создания задачи", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, example = "Задача успешно отправлена на обработку")
                                        String message) {

    public TaskProcessingResponseDTO {
        if (StringUtils.isBlank(message)) {
            throw new IllegalArgumentException("Сообщение в ответе необходимо");
        }
    }
}
