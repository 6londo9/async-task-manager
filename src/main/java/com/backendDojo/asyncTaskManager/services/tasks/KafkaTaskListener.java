package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaTaskListener {

    @KafkaListener(groupId = "task-group", topics = {"tasks"})
    public void consume(TaskRequestDTO taskRequestDTO) {

    }
}
