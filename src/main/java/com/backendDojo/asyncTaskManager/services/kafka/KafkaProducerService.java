package com.backendDojo.asyncTaskManager.services.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService<V> {

    private final KafkaTemplate<String, V> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, V> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, V value) {
        kafkaTemplate.send(topic, value);
    }
}
