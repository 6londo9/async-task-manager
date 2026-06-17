package com.backendDojo.asyncTaskManager.models.dtos.kafka;

public record DlqMessageDto(String data,
                            String errorMessage) {
}
