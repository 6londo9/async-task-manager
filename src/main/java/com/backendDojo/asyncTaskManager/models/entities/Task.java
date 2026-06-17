package com.backendDojo.asyncTaskManager.models.entities;

import com.backendDojo.asyncTaskManager.models.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long duration;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String result;

    private Long userId;

    private int retryCount;

    private OffsetDateTime startedAt;

    private OffsetDateTime createdAt;

    @Version
    private Long version;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCounter() {
        this.retryCount = this.retryCount + 1;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @PrePersist
    protected void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return retryCount == task.retryCount && Objects.equals(id, task.id) && Objects.equals(name, task.name) && Objects.equals(duration, task.duration) && status == task.status && Objects.equals(result, task.result) && Objects.equals(userId, task.userId) && Objects.equals(startedAt, task.startedAt) && Objects.equals(createdAt, task.createdAt) && Objects.equals(version, task.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, duration, status, result, userId, retryCount, startedAt, createdAt, version);
    }
}
