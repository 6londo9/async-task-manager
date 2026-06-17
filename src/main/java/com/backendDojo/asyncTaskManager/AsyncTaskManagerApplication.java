package com.backendDojo.asyncTaskManager;

import com.backendDojo.asyncTaskManager.configs.kafka.KafkaTopicProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KafkaTopicProperties.class)
@EnableKafkaStreams
public class AsyncTaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncTaskManagerApplication.class, args);
    }
}
