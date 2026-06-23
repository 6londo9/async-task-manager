package com.backendDojo.asyncTaskManager.exceptions;

public class NotificationInboxNotFoundException extends RuntimeException {
    public NotificationInboxNotFoundException(Long notificationId) {
        super("Notification with id " + notificationId + " not found");
    }
}
