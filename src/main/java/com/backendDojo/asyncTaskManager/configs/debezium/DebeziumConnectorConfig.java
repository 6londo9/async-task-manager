package com.backendDojo.asyncTaskManager.configs.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumConnectorConfig {

    @JsonProperty("name")
    private String name;

    @JsonProperty("config")
    private DebeziumConnectorConfigProperties config;

    public DebeziumConnectorConfig(String name, DebeziumConnectorConfigProperties config) {
        this.name = name;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public DebeziumConnectorConfigProperties getConfig() {
        return config;
    }
}
