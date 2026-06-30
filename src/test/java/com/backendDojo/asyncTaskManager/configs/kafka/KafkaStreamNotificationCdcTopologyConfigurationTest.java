package com.backendDojo.asyncTaskManager.configs.kafka;

import com.backendDojo.asyncTaskManager.models.dtos.kafka.NotificationMessage;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.errors.StreamsException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaStreamNotificationCdcTopologyConfigurationTest {

    private static final String CDC_TOPIC = "cdc.public.notifications";
    private static final String NOTIFICATIONS_TOPIC = "notifications";

    private final JsonMapper mapper = new JsonMapper();

    @Test
    void mapsWrappedDebeziumCreateEventToNotificationMessage() throws Exception {
        try (TopologyTestDriver driver = newDriver()) {
            TestInputTopic<String, JsonNode> input = inputTopic(driver);
            TestOutputTopic<String, NotificationMessage> output = outputTopic(driver);

            input.pipeInput("source-key", json("""
                    {
                      "schema": {},
                      "payload": {
                        "op": "c",
                        "after": { "id": 42 }
                      }
                    }
                    """));

            KeyValue<String, NotificationMessage> record = output.readKeyValue();
            assertEquals("42", record.key);
            assertEquals(42L, record.value.notificationId());
        }
    }

    @Test
    void mapsUnwrappedDebeziumCreateEventToNotificationMessage() throws Exception {
        try (TopologyTestDriver driver = newDriver()) {
            TestInputTopic<String, JsonNode> input = inputTopic(driver);
            TestOutputTopic<String, NotificationMessage> output = outputTopic(driver);

            input.pipeInput("source-key", json("""
                    {
                      "op": "c",
                      "after": { "id": 43 }
                    }
                    """));

            assertEquals(43L, output.readValue().notificationId());
        }
    }

    @Test
    void ignoresNonCreateDebeziumEvents() throws Exception {
        try (TopologyTestDriver driver = newDriver()) {
            TestInputTopic<String, JsonNode> input = inputTopic(driver);
            TestOutputTopic<String, NotificationMessage> output = outputTopic(driver);

            input.pipeInput("source-key", json("""
                    {
                      "op": "u",
                      "after": { "id": 42 }
                    }
                    """));

            assertTrue(output.isEmpty());
        }
    }

    @Test
    void rejectsMalformedCreateEvent() throws Exception {
        try (TopologyTestDriver driver = newDriver()) {
            TestInputTopic<String, JsonNode> input = inputTopic(driver);

            StreamsException exception = assertThrows(StreamsException.class, () -> input.pipeInput("source-key", json("""
                    {
                      "op": "c",
                      "after": { "message": "missing id" }
                    }
                    """)));
            assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
        }
    }

    private TopologyTestDriver newDriver() {
        KafkaTopicProperties topics = new KafkaTopicProperties();
        topics.setNotificationsCdc(CDC_TOPIC);
        topics.setNotifications(NOTIFICATIONS_TOPIC);

        StreamsBuilder builder = new StreamsBuilder();
        new KafkaStreamNotificationCdcTopologyConfiguration().notificationStream(builder, topics, mapper);

        Properties properties = new Properties();
        properties.put("application.id", "topology-test");
        properties.put("bootstrap.servers", "dummy:9092");
        properties.put("default.key.serde", Serdes.String().getClass().getName());
        properties.put("default.value.serde", Serdes.String().getClass().getName());
        return new TopologyTestDriver(builder.build(), properties);
    }

    private TestInputTopic<String, JsonNode> inputTopic(TopologyTestDriver driver) {
        return driver.createInputTopic(
                CDC_TOPIC,
                new StringSerializer(),
                new JacksonJsonSerializer<>(mapper)
        );
    }

    private TestOutputTopic<String, NotificationMessage> outputTopic(TopologyTestDriver driver) {
        return driver.createOutputTopic(
                NOTIFICATIONS_TOPIC,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(NotificationMessage.class, mapper)
        );
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }
}
