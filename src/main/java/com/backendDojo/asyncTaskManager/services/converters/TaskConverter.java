package com.backendDojo.asyncTaskManager.services.converters;

import com.backendDojo.asyncTaskManager.models.dtos.TaskResponseDTO;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskConverter {

    TaskResponseDTO convertToResponseDto(Task task);
}
