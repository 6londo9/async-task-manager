package com.backendDojo.asyncTaskManager.models.dtos;

public record DlqMessageDto(String data,
                            String errorMessage) {
}
