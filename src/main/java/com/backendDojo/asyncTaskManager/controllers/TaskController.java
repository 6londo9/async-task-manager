package com.backendDojo.asyncTaskManager.controllers;

import com.backendDojo.asyncTaskManager.exceptions.TaskNotFoundException;
import com.backendDojo.asyncTaskManager.models.dtos.ErrorResponse;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import com.backendDojo.asyncTaskManager.models.dtos.TaskResponseDTO;
import com.backendDojo.asyncTaskManager.services.converters.TaskConverter;
import com.backendDojo.asyncTaskManager.services.tasks.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            operationId = "addTaskForProcessing",
            summary = "Добавить задачу в очередь на обработку",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = TaskRequestDTO.class))
            )
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Задача успешно добавлена в очередь на обработку",
                content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректное тело запроса",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<String> addTaskForProcessing(@RequestBody @Valid TaskRequestDTO taskRequestDTO) {
        taskService.publishTask(taskRequestDTO);
        return new ResponseEntity<>("Задача принята в обработку", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "addTaskForProcessing",
            summary = "Получить статус задачи"
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Задача успешно добавлена в очередь на обработку",
                content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные параметры запроса",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Задача не найдена",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<TaskResponseDTO> getTask(@Parameter(description = "Идентификатор задачи", required = true, example = "42")
                                                   @PathVariable Long id) {
        return taskService.findById(id)
                .map(task -> new ResponseEntity<>(taskConverter.convertToResponseDto(task), HttpStatus.OK))
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
