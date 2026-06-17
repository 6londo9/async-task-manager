package com.backendDojo.asyncTaskManager.services.converters;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import com.backendDojo.asyncTaskManager.models.entities.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface NotificationConverter {

    @Mappings({
            @Mapping(source = "id.userId", target = "userId"),
            @Mapping(source = "id.taskId", target = "taskId")
    })
    NotificationMessage convertToMessage(Notification notification);
}
