package com.backendDojo.asyncTaskManager.configs.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.tasks}")
    private String tasksTopicName;

    @Value("${kafka.topics.tasks-dlq}")
    private String tasksDlqTopicName;

    @Bean
    public NewTopic tasksTopic() {
        return TopicBuilder
                .name(tasksTopicName)
                .build();
    }

    @Bean
    public NewTopic tasksDlqTopic() {
        return TopicBuilder
                .name(tasksDlqTopicName)
                .build();
    }
}
