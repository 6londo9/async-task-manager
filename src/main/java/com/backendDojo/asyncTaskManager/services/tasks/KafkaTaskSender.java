package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.models.dtos.DlqMessageDto;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.services.kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class KafkaTaskSender {

    @Value("${kafka.topics.tasks}")
    private String tasksTopicName;
    @Value("${kafka.topics.tasks-dlq}")
    private String tasksDlqTopicName;

    private final KafkaProducerService<TaskRequestDTO> kafkaProducerService;
    private final KafkaProducerService<DlqMessageDto> dlqKafkaProducerService;
    private final ObjectMapper mapper;

    public KafkaTaskSender(KafkaProducerService<TaskRequestDTO> kafkaProducerService,
                           KafkaProducerService<DlqMessageDto> dlqKafkaProducerService,
                           ObjectMapper mapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.dlqKafkaProducerService = dlqKafkaProducerService;
        this.mapper = mapper;
    }

    public void publishTask(TaskRequestDTO taskRequestDTO) {
        kafkaProducerService.send(tasksTopicName, taskRequestDTO);
    }

    public void publishErrorTaskToDlq(Task task, Exception ex) {
        DlqMessageDto dlqMessage = new DlqMessageDto(mapper.writeValueAsString(task), ex.getMessage());
        dlqKafkaProducerService.send(tasksDlqTopicName, dlqMessage);
    }
}
