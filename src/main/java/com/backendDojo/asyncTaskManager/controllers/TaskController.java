package com.backendDojo.asyncTaskManager.controllers;

import com.backendDojo.asyncTaskManager.annotations.swagger.api.TasksApi;
import com.backendDojo.asyncTaskManager.annotations.swagger.operations.AddTaskForProcessing;
import com.backendDojo.asyncTaskManager.annotations.swagger.operations.GetTaskById;
import com.backendDojo.asyncTaskManager.annotations.swagger.operations.GetUsersTasks;
import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.TaskProcessingResponseDTO;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.TaskResponseDTO;
import com.backendDojo.asyncTaskManager.services.converters.TaskConverter;
import com.backendDojo.asyncTaskManager.services.tasks.TaskService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.AUTH_HEADER_NAME;

@RestController
@RequestMapping("/api/tasks")
@TasksApi
public class TaskController {

    private final TaskService taskService;
    private final TaskConverter taskConverter;

    public TaskController(TaskService taskService, TaskConverter taskConverter) {
        this.taskService = taskService;
        this.taskConverter = taskConverter;
    }

    @PostMapping
    @AddTaskForProcessing
    public ResponseEntity<TaskProcessingResponseDTO> addTaskForProcessing(@Parameter(hidden = true)
                                                                          @RequestHeader(name = AUTH_HEADER_NAME)
                                                                          Long userId,
                                                                          @RequestBody
                                                                          @Valid
                                                                          TaskRequestDTO taskRequestDTO) {
        taskService.publishTask(userId, taskRequestDTO);
        return new ResponseEntity<>(new TaskProcessingResponseDTO("Задача принята в обработку"), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @GetTaskById
    public ResponseEntity<TaskResponseDTO> getTask(@Parameter(hidden = true)
                                                   @RequestHeader(name = AUTH_HEADER_NAME)
                                                   Long userId,
                                                   @Parameter(description = "Идентификатор задачи", required = true, example = "42", schema = @Schema(implementation = Long.class))
                                                   @PathVariable
                                                   Long id) {
        return taskService.findById(id, userId)
                .map(task -> new ResponseEntity<>(taskConverter.convertToResponseDto(task), HttpStatus.OK))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @GetMapping
    @GetUsersTasks
    public ResponseEntity<List<TaskResponseDTO>> getUsersTasks(@Parameter(hidden = true)
                                                               @RequestHeader(name = AUTH_HEADER_NAME)
                                                               Long userId) {
        return new ResponseEntity<>(
                taskService.findUsersTask(userId)
                        .stream()
                        .map(taskConverter::convertToResponseDto)
                        .sorted(Comparator.comparing(TaskResponseDTO::id))
                        .toList(),
                HttpStatus.OK
        );
    }
}
