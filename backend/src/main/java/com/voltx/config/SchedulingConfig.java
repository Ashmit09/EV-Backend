package com.voltx.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs are switched off in tests (voltx.scheduler.enabled=false) for determinism. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "voltx.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
