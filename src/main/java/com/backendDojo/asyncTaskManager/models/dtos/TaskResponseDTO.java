package com.backendDojo.asyncTaskManager.models.dtos;

public record TaskResponseDTO(Long id,
                              String name,
                              String status,
                              String result) {
}
