package com.backendDojo.asyncTaskManager.exceptions;

public class TaskStallException extends RuntimeException {
    public TaskStallException(int retryCount) {
        super("Task exceeded maximum retry count: " + retryCount);
    }
}
