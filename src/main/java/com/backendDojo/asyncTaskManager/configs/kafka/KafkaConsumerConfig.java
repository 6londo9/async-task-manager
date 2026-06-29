package com.backendDojo.asyncTaskManager.configs.kafka;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.CreateTaskMessage;
import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${app.kafka.tasks.consumers.group-id}")
    private String taskGroupId;
    @Value("${app.kafka.notifications.consumers.group-id}")
    private String notificationGroupId;

    @Bean
    public ConsumerFactory<String, CreateTaskMessage> taskConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, taskGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JacksonJsonDeserializer<>(CreateTaskMessage.class));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, CreateTaskMessage>> kafkaTaskListenerContainerFactory(ConsumerFactory<String, CreateTaskMessage> taskConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, CreateTaskMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(taskConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, NotificationMessage> notificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, notificationGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JacksonJsonDeserializer<>(NotificationMessage.class));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, NotificationMessage>> kafkaNotificationListenerContainerFactory(ConsumerFactory<String, NotificationMessage> notificationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, NotificationMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory);
        return factory;
    }
}
