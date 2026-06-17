package com.backendDojo.asyncTaskManager.configs.debezium;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class DebeziumConfig {

    private static final Logger log = LoggerFactory.getLogger(DebeziumConfig.class);

    @Value("${app.debezium.url}")
    private String debeziumUrl;
    @Value("${app.debezium.name}")
    private String debeziumConfigName;
    @Value("${app.debezium.config.connector.class}")
    private String debeziumConnectorClass;
    @Value("${app.debezium.config.tasks.max}")
    private String debeziumMaxTasks;
    @Value("${app.debezium.config.plugin.name}")
    private String debeziumPluginName;
    @Value("${app.debezium.config.database.hostname}")
    private String debeziumDatabaseHost;
    @Value("${app.debezium.config.database.port}")
    private String debeziumDatabasePort;
    @Value("${app.debezium.config.database.user}")
    private String debeziumDatabaseUser;
    @Value("${app.debezium.config.database.password}")
    private String debeziumDatabasePassword;
    @Value("${app.debezium.config.database.dbname}")
    private String debeziumDatabaseName;
    @Value("${app.debezium.config.topic.prefix}")
    private String debeziumTopicPrefix;
    @Value("${app.debezium.config.table.include.list}")
    private String debeziumTable;
    @Value("${app.debezium.config.snapshot.mode}")
    private String debeziumSnapshotMode;

    @Bean
    public CommandLineRunner registerConnector(RestClient.Builder debeziumRestClientBuilder, ObjectMapper mapper) {
        return args -> {
            RestClient debeziumRestClient = debeziumRestClientBuilder.
                    baseUrl(debeziumUrl + "/connectors")
                    .build();

            DebeziumConnectorConfig config = this.getDebeziumConnectorConfig();

            try {
                String response = debeziumRestClient.post()
                        .body(mapper.writeValueAsString(config))
                        .contentType(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);
                log.debug("Debezium connector config posted successfully: {}", response);
            } catch (Exception e) {
                log.warn("Debezium connector config post failed: {}", e.getMessage());
            }
        };
    }

    private DebeziumConnectorConfig getDebeziumConnectorConfig() {
        DebeziumConnectorConfigProperties properties = new DebeziumConnectorConfigProperties(
                debeziumConnectorClass,
                debeziumMaxTasks,
                debeziumPluginName,
                debeziumDatabaseHost,
                debeziumDatabasePort,
                debeziumDatabaseUser,
                debeziumDatabasePassword,
                debeziumDatabaseName,
                debeziumTopicPrefix,
                debeziumTable,
                debeziumSnapshotMode
        );
        return new DebeziumConnectorConfig(debeziumConfigName, properties);
    }
}
