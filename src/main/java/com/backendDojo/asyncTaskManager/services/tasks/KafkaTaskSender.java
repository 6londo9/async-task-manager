package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.configs.kafka.KafkaTopicProperties;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.services.kafka.KafkaProducerService;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.Message;
import org.springframework.stereotype.Service;

@Service
public class KafkaTaskSender {

    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaProducerService kafkaProducerService;

    public KafkaTaskSender(KafkaTopicProperties kafkaTopicProperties, KafkaProducerService kafkaProducerService) {
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.kafkaProducerService = kafkaProducerService;
    }

    public void publishTask(TaskRequestDTO taskRequestDTO) {
        kafkaProducerService.send(new Message(kafkaTopicProperties.getTasks(), taskRequestDTO));
    }
}
