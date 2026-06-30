package com.backendDojo.asyncTaskManager.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
//@Profile("!tests")
@ConditionalOnBooleanProperty(name = "app.scheduler.enabled", matchIfMissing = true)
public class SchedulerConfig {
}
