package com.backendDojo.asyncTaskManager.annotations.swagger.api;


import com.backendDojo.asyncTaskManager.annotations.swagger.security.BaseUserIdSecurity;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@BaseUserIdSecurity
@Tag(name = "Task API", description = "API для работы с задачами")
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TasksApi {
}
