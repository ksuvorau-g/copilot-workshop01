package com.example.aidemo1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration to enable Spring Retry support for automatic retry logic.
 * 
 * <p>Enables the use of @Retryable annotation in provider implementations
 * for handling transient failures.</p>
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
