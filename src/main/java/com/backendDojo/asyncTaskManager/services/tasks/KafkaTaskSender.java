package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.configs.kafka.KafkaTopicProperties;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaTaskSender {

    private final KafkaTopicProperties kafkaTopicProperties;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaTaskSender(KafkaTopicProperties kafkaTopicProperties, KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTask(Long userId, TaskRequestDTO taskRequestDTO) {
        kafkaTemplate.send(
                kafkaTopicProperties.getTasks(),
                new CreateTaskMessage(
                        taskRequestDTO.name(),
                        taskRequestDTO.duration(),
                        userId
                )
        );
    }
}
