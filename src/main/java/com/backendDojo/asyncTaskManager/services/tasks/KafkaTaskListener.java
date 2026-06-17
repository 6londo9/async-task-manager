package com.backendDojo.asyncTaskManager.services.tasks;

import com.backendDojo.asyncTaskManager.exceptions.TaskAlreadyExistsException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.entities.Task;
import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaTaskListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaTaskListener.class);

    private final TaskService taskService;

    public KafkaTaskListener(TaskService taskService) {
        this.taskService = taskService;
    }

    @KafkaListener(
            groupId = "${app.kafka.tasks.consumers.group-id}",
            topics = {"${app.kafka.topics.tasks}"},
            containerFactory = "kafkaTaskListenerContainerFactory"
    )
    @Transactional
    public void consume(TaskRequestDTO taskRequestDTO) {
        log.info("Received task request: {}", taskRequestDTO);
        try {
            taskService.saveTask(taskRequestDTO);
        } catch (TaskAlreadyExistsException ex) {
            log.info("Task with name {}, which made by user {} already exists. Skipping", taskRequestDTO.name(), taskRequestDTO.userId());
        }
    }
}
