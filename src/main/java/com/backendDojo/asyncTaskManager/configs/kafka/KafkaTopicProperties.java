package com.backendDojo.asyncTaskManager.configs.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka.topics")
public class KafkaTopicProperties {

    private String tasks;
    private String notifications;
    private String notificationsCdc;

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }

    public String getNotificationsCdc() {
        return notificationsCdc;
    }

    public void setNotificationsCdc(String notificationsCdc) {
        this.notificationsCdc = notificationsCdc;
    }
}
