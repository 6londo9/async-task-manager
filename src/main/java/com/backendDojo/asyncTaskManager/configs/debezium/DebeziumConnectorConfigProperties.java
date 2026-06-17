package com.backendDojo.asyncTaskManager.configs.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumConnectorConfigProperties {

    @JsonProperty("connector.class")
    private String connectorClass;

    @JsonProperty("tasks.max")
    private String maxTasks;

    @JsonProperty("plugin.name")
    private String pluginName;

    @JsonProperty("database.hostname")
    private String databaseHost;

    @JsonProperty("database.port")
    private String databasePort;

    @JsonProperty("database.user")
    private String databaseUser;

    @JsonProperty("database.password")
    private String databasePassword;

    @JsonProperty("database.dbname")
    private String databaseName;

    @JsonProperty("topic.prefix")
    private String topicPrefix;

    @JsonProperty("table.include.list")
    private String table;

    @JsonProperty("snapshot.mode")
    private String snapshotMode;

    public DebeziumConnectorConfigProperties(String connectorClass, String maxTasks, String pluginName, String databaseHost, String databasePort, String databaseUser, String databasePassword, String databaseName, String topicPrefix, String table, String snapshotMode) {
        this.connectorClass = connectorClass;
        this.maxTasks = maxTasks;
        this.pluginName = pluginName;
        this.databaseHost = databaseHost;
        this.databasePort = databasePort;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.databaseName = databaseName;
        this.topicPrefix = topicPrefix;
        this.table = table;
        this.snapshotMode = snapshotMode;
    }

    public String getConnectorClass() {
        return connectorClass;
    }

    public String getMaxTasks() {
        return maxTasks;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public String getDatabasePort() {
        return databasePort;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public String getTable() {
        return table;
    }

    public String getSnapshotMode() {
        return snapshotMode;
    }
}
