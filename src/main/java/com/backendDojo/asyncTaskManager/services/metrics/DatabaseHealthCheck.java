package com.backendDojo.asyncTaskManager.services.metrics;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthCheck extends AbstractHealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1000)) {
                builder.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "UP");
            } else {
                builder.down()
                        .withDetail("error", "Database connection is invalid");
            }
        } catch (SQLException e) {
            builder.down()
                    .withDetail("error", e.getMessage());
        }
    }
}
