package com.backendDojo.asyncTaskManager.services.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TaskMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter optimisticLockConflicts;

    public TaskMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.optimisticLockConflicts = Counter.builder("task.conflicts")
                .description("Number of optimistic lock conflicts")
                .register(meterRegistry);
    }

    public void recordConflict() {
        optimisticLockConflicts.increment();
    }
}
