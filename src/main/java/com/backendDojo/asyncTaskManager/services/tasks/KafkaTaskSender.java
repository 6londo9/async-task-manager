package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.services.kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KafkaTaskSender {

    @Value("${kafka.topics.tasks}")
    private String tasksTopicName;

    private final KafkaProducerService<TaskRequestDTO> kafkaProducerService;

    public KafkaTaskSender(KafkaProducerService<TaskRequestDTO> kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    public void publishTask(TaskRequestDTO taskRequestDTO) {
        kafkaProducerService.send(tasksTopicName, taskRequestDTO);
    }
}
