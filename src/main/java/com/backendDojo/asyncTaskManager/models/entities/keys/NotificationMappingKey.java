package com.backendDojo.asyncTaskManager.models.entities.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class NotificationMappingKey implements Serializable {

    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
