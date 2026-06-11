package com.backendDojo.asyncTaskManager.services.metrics;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConnectionPoolMonitor {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPoolMonitor.class);

    private final HikariDataSource dataSource;
    private final MeterRegistry meterRegistry;

    public ConnectionPoolMonitor(HikariDataSource dataSource, MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 60000) // каждую минуту
    public void monitorConnectionPool() {
        HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();

        log.info("Connection pool status: active={}, idle={}, total={}",
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getTotalConnections()
        );

        meterRegistry.gauge("hikari.pool.active.connections",
                poolMXBean.getActiveConnections());
        meterRegistry.gauge("hikari.pool.idle.connections",
                poolMXBean.getIdleConnections());
    }
}
