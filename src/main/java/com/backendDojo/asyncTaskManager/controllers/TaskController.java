package com.backendDojo.asyncTaskManager.controllers;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.TaskResponseDTO;
import com.backendDojo.asyncTaskManager.services.converters.TaskConverter;
import com.backendDojo.asyncTaskManager.services.tasks.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task API", description = "API для работы с задачами")
public class TaskController {

    private final TaskService taskService;
    private final TaskConverter taskConverter;

    public TaskController(TaskService taskService, TaskConverter taskConverter) {
        this.taskService = taskService;
        this.taskConverter = taskConverter;
    }

    @PostMapping
    @Operation(summary = "Добавить задачу в очередь на обработку")
    public ResponseEntity<String> addTaskForProcessing(@RequestBody @Valid TaskRequestDTO taskRequestDTO) {
        taskService.publishTask(taskRequestDTO);
        return new ResponseEntity<>("Задача принята в обработку", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить статус задачи")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable Long id) {
        return taskService.findById(id)
                .map(task -> new ResponseEntity<>(taskConverter.convertToResponseDto(task), HttpStatus.OK))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
