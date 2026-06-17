package com.backendDojo.asyncTaskManager.models.entities;

import com.backendDojo.asyncTaskManager.models.entities.keys.NotificationMappingKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Table(name = "notifications_inbox")
@Entity
public class NotificationInbox {

    @EmbeddedId
    private NotificationMappingKey id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "task_id", referencedColumnName = "task_id"),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    })
    private Notification notification;

    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    public NotificationMappingKey getId() {
        return id;
    }

    public void setId(NotificationMappingKey id) {
        this.id = id;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NotificationInbox that = (NotificationInbox) o;
        return isProcessed == that.isProcessed && Objects.equals(id, that.id) && Objects.equals(startedAt, that.startedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isProcessed, startedAt);
    }
}
