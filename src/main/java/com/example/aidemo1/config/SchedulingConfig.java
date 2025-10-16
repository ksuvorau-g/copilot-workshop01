package com.example.aidemo1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class for enabling scheduled tasks.
 * 
 * <p>This configuration enables Spring's scheduled task execution capability,
 * allowing methods annotated with {@code @Scheduled} to run at specified intervals.</p>
 * 
 * <h2>Scheduler Thread Pool</h2>
 * <p>The thread pool size and naming prefix are configured in application.properties:</p>
 * <ul>
 *   <li>{@code spring.task.scheduling.pool.size=5}</li>
 *   <li>{@code spring.task.scheduling.thread-name-prefix=scheduler-}</li>
 * </ul>
 * 
 * @see org.springframework.scheduling.annotation.EnableScheduling
 * @see org.springframework.scheduling.annotation.Scheduled
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Configuration is primarily declarative via @EnableScheduling
    // Specific scheduler settings are in application.properties
}
