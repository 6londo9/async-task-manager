package com.backendDojo.asyncTaskManager.configs.kafka;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaStreamNotificationCdcTopologyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamNotificationCdcTopologyConfiguration.class);

    @Bean
    public KStream<String, NotificationMessage> notificationStream(
            StreamsBuilder streamsBuilder,
            KafkaTopicProperties kafkaTopicProperties,
            ObjectMapper mapper
    ) {
        try (JacksonJsonSerde<JsonNode> debeziumSerde = new JacksonJsonSerde<>(JsonNode.class, (JsonMapper) mapper);
             JacksonJsonSerde<NotificationMessage> outputSerde = new JacksonJsonSerde<>(NotificationMessage.class, (JsonMapper) mapper)) {
            KStream<String, JsonNode> notificationsCdcStream = streamsBuilder
                    .stream(
                            kafkaTopicProperties.getNotificationsCdc(),
                            Consumed.with(Serdes.String(), debeziumSerde)
                    );

            KStream<String, NotificationMessage> notificationMessages =
                    notificationsCdcStream
                            .filter((key, event) -> event != null)
                            .filter((key, event) ->
                                    "c".equals(getPayloadNode(event).path("op").asString())
                            )
                            .mapValues(event -> {
                                JsonNode after =
                                        getPayloadNode(event).path("after");

                                if (!after.isObject()) {
                                    throw new IllegalArgumentException(
                                            "Debezium create event has no after object"
                                    );
                                }

                                return new NotificationMessage(
                                        requiredLong(after, "task_id"),
                                        requiredLong(after, "user_id")
                                );
                            })
                            .selectKey((sourceKey, message) ->
                                    String.valueOf(message.taskId())
                            );

            notificationMessages.to(
                    kafkaTopicProperties.getNotifications(),
                    Produced.with(Serdes.String(), outputSerde)
            );

            return notificationMessages;
        } catch (Exception ex) {
            log.error("Error occurred while processing kafka stream notification streams: {}", ex.getMessage());
            throw ex;
        }
    }

    private static JsonNode getPayloadNode(JsonNode root) {
        if (root.has("schema") && root.path("payload").isObject()) {
            return root.path("payload");
        }

        return root;
    }

    private static Long requiredLong(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || !value.canConvertToLong()) {
            throw new IllegalArgumentException(
                    "Missing or invalid field: " + fieldName
            );
        }

        return value.longValue();
    }
}
