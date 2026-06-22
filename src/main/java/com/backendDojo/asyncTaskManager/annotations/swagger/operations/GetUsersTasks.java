package com.backendDojo.asyncTaskManager.annotations.swagger.operations;


import com.backendDojo.asyncTaskManager.models.dtos.ErrorResponse;
import com.backendDojo.asyncTaskManager.models.dtos.TaskResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
        operationId = "getUserTasks",
        summary = "Получить задачи пользователя"
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Задачи успешно получены",
                content = @Content(
                        mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = TaskResponseDTO.class)),
                        examples = @ExampleObject(
                                name = "Успешный результат",
                                value = "[ { \"id\": 1, \"name\": \"Very urgent task\", \"status\": \"COMPLETED\", \"result\": \"Задача завершена успешно\", \"userId\": 123 } ]"
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
public @interface GetUsersTasks {
}
