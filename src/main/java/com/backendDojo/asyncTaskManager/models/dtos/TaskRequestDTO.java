package com.backendDojo.asyncTaskManager.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Random;

@Schema(description = "Запрос на создание задачи")
public record TaskRequestDTO(@Schema(description = "Название задачи", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, example = "Clean room")
                             @NotBlank(message = "Название задачи обязательно")
                             String name,
                             @Schema(description = "Длительность выполнения задачи", minimum = "0", maximum = "50000", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "100")
                             @Positive(message = "Длительность должна быть положительной")
                             Long duration) {

    public TaskRequestDTO {
        Random random = new Random();
        if (duration == null) {
            duration = random.nextLong(1, 50_000);
        }
    }
}