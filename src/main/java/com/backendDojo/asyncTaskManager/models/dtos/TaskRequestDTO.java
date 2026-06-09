package com.backendDojo.asyncTaskManager.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TaskRequestDTO(
        @NotBlank(message = "Название задачи обязательно")
        String name,
        @Positive(message = "Длительность должна быть положительной")
        Long duration) {
}
