package com.backendDojo.asyncTaskManager;

import com.backendDojo.asyncTaskManager.repositories.NotificationInboxRepository;
import com.backendDojo.asyncTaskManager.repositories.NotificationRepository;
import com.backendDojo.asyncTaskManager.repositories.TaskRepository;
import com.redis.testcontainers.RedisContainer;
import io.debezium.testing.testcontainers.DebeziumContainer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = {"tests"})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractIntegrationTest {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka:19092";

    private static final int CONTENDERS = 12;

    static Network network = Network.newNetwork();

    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected NotificationRepository notificationRepository;
    @Autowired
    protected NotificationInboxRepository notificationInboxRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:14.7-alpine3.17")
            .withDatabaseName("tasksdb")
            .withUsername("user")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withCommand("postgres", "-c", "wal_level=logical");

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer("redis:latest")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withCommand("--requirepass", "password", "--appendonly", "yes");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:latest")
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener(KAFKA_BOOTSTRAP_SERVERS);

    @Container
    static DebeziumContainer debezium = new DebeziumContainer("debezium/connect:2.7.3.Final")
            .withKafka(network, KAFKA_BOOTSTRAP_SERVERS)
            .dependsOn(kafka, postgres);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.debezium.url", debezium::getTarget);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @AfterEach
    void afterEach() {
        notificationInboxRepository.deleteAll();
        notificationRepository.deleteAll();
        taskRepository.deleteAll();
    }

    protected void runConcurrently(Runnable operation) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(CONTENDERS)) {
            CountDownLatch ready = new CountDownLatch(CONTENDERS);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(CONTENDERS);
            AtomicReference<Exception> firstFailure = new AtomicReference<>();
            for (int i = 0; i < CONTENDERS; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        assertTrue(start.await(5, TimeUnit.SECONDS));
                        operation.run();
                    } catch (Exception ex) {
                        firstFailure.compareAndSet(null, ex);
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            executor.shutdownNow();
            assertNull(firstFailure.get());
        }
    }
}
