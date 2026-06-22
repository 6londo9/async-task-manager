package com.backendDojo.asyncTaskManager.annotations.swagger.operations;

import com.backendDojo.asyncTaskManager.models.dtos.ErrorResponse;
import com.backendDojo.asyncTaskManager.models.dtos.TaskProcessingResponseDTO;
import com.backendDojo.asyncTaskManager.models.dtos.TaskRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.AUTH_HEADER_NAME;
import static java.lang.annotation.ElementType.METHOD;

@SecurityRequirement(name = AUTH_HEADER_NAME)
@Operation(
        operationId = "addTaskForProcessing",
        summary = "Добавить задачу в очередь на обработку",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = TaskRequestDTO.class),
                        examples = @ExampleObject(
                                name = "Запрос на создание задачи",
                                value = "{ \"name\": \"Very urgent task\" }"
                        )
                )
        )
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Задача успешно добавлена в очередь на обработку",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = TaskProcessingResponseDTO.class),
                        examples = @ExampleObject(
                                name = "Успешный результат",
                                value = "{ \"message\": \"Задача принята в обработку\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Некорректные параметры запроса",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "Ответ с информацией об ошибке",
                                value = "{ \"message\": \"В запросе отсутствует идентификатор задачи\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Недостаточно прав",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "Ответ с информацией об ошибке",
                                value = "{ \"message\": \"У пользователя нет прав для выполнения этого действия\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Внутренняя ошибка сервера",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "Ответ с информацией об ошибке",
                                value = "{ \"message\": \"Внутренняя ошибка сервера\" }"
                        )
                )
        )
})
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AddTaskForProcessing {
}
