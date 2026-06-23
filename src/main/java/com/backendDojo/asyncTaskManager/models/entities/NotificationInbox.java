package com.backendDojo.asyncTaskManager.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Table(name = "notifications_inbox")
@Entity
public class NotificationInbox {

    @Id
    private Long notificationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", referencedColumnName = "id")
    private Notification notification;

    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
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
        return isProcessed == that.isProcessed && Objects.equals(notificationId, that.notificationId) && Objects.equals(startedAt, that.startedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId, isProcessed, startedAt);
    }
}
