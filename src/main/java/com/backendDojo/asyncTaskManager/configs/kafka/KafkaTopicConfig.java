package com.backendDojo.asyncTaskManager.configs.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    public KafkaTopicConfig(KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Bean
    public NewTopic tasksTopic() {
        return TopicBuilder
                .name(kafkaTopicProperties.getTasks())
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder
                .name(kafkaTopicProperties.getNotifications())
                .build();
    }

    @Bean
    public NewTopic notificationsCdcTopic() {
        return TopicBuilder
                .name(kafkaTopicProperties.getNotificationsCdc())
                .build();
    }
}
