package com.example.aidemo1.scheduler;

import com.example.aidemo1.service.ExchangeRateCacheService;
import com.example.aidemo1.service.ExchangeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduled task for refreshing exchange rates from external providers.
 *
 * <p>This scheduler periodically fetches fresh exchange rates from all configured
 * providers and updates the database and Redis cache to ensure users always have 
 * access to current rate information.</p>
 *
 * <h2>Schedule Configuration</h2>
 * <ul>
 *   <li><strong>Frequency:</strong> Every hour (0 0 * * * *)</li>
 *   <li><strong>Initial Delay:</strong> 60 seconds after application startup</li>
 *   <li><strong>Thread Pool:</strong> Configured in application.properties (pool size: 5)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>Exceptions are caught and logged to prevent the scheduler from stopping.
 * If a refresh fails, the scheduler will retry on the next scheduled execution.
 * Redis failures are logged but do not halt the refresh process.</p>
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Log the start of the refresh process</li>
 *   <li>Invalidate all Redis cache</li>
 *   <li>Call {@link ExchangeRateService#refreshAllRates()}</li>
 *   <li>Log the number of successfully refreshed currency pairs</li>
 *   <li>Log completion time and duration</li>
 *   <li>If error occurs, log the exception and continue</li>
 * </ol>
 *
 * @see ExchangeRateService#refreshAllRates()
 * @see ExchangeRateCacheService#invalidateAllCache()
 * @see Scheduled
 */
@Slf4j
@Component
public class ExchangeRateScheduler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateCacheService cacheService;

    /**
     * Constructs the scheduler with required dependencies.
     *
     * @param exchangeRateService service for refreshing exchange rates
     * @param cacheService service for Redis cache operations
     */
    public ExchangeRateScheduler(ExchangeRateService exchangeRateService, 
                                 ExchangeRateCacheService cacheService) {
        this.exchangeRateService = exchangeRateService;
        this.cacheService = cacheService;
        log.info("ExchangeRateScheduler initialized. Scheduled to run every hour at the top of the hour.");
    }

    /**
     * Scheduled task to refresh all exchange rates.
     *
     * <p>This method runs every hour (3600000ms) with an initial delay of 10 seconds after startup.</p>
     *
     * <p>Note: By default, Spring's @Scheduled annotation runs tasks sequentially
     * in a single thread, so concurrent executions are prevented automatically.</p>
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 10000) // Every hour, starting 10 seconds after startup
    public void refreshExchangeRates() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("=== Starting scheduled exchange rate refresh at {} ===",
                startTime.format(TIME_FORMATTER));

        try {
            // Step 1: Invalidate all Redis cache
            try {
                log.info("Invalidating all cached exchange rates");
                cacheService.invalidateAllCache();
                log.info("Successfully invalidated all cached exchange rates");
            } catch (Exception e) {
                log.error("Failed to invalidate Redis cache: {}", e.getMessage(), e);
                log.warn("Continuing with rate refresh despite cache invalidation failure");
            }

            // Step 2: Call the service to refresh all rates
            int refreshedCount = exchangeRateService.refreshAllRates();

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            log.info("=== Exchange rate refresh completed successfully ===");
            log.info("Currency pairs refreshed: {}", refreshedCount);
            log.info("Completion time: {}", endTime.format(TIME_FORMATTER));
            log.info("Duration: {} seconds", durationSeconds);

        } catch (Exception e) {
            LocalDateTime errorTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, errorTime).getSeconds();

            log.error("=== Exchange rate refresh FAILED ===");
            log.error("Error occurred at: {}", errorTime.format(TIME_FORMATTER));
            log.error("Duration before failure: {} seconds", durationSeconds);
            log.error("Error details: {}", e.getMessage(), e);
            log.warn("Scheduler will retry on next scheduled execution (in 1 hour)");

            // Do not rethrow - allow scheduler to continue running
        }
    }

    /**
     * Manual trigger for exchange rate refresh.
     *
     * <p>This method can be called programmatically or via an admin endpoint
     * to force an immediate refresh outside the scheduled intervals. It also
     * invalidates the Redis cache before refreshing.</p>
     *
     * @return the number of successfully refreshed currency pairs
     * @throws RuntimeException if the refresh fails
     */
    public int triggerManualRefresh() {
        log.info("Manual refresh triggered by external call");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Invalidate cache before manual refresh
            try {
                log.info("Invalidating all cached exchange rates (manual refresh)");
                cacheService.invalidateAllCache();
                log.info("Successfully invalidated all cached exchange rates");
            } catch (Exception e) {
                log.error("Failed to invalidate Redis cache during manual refresh: {}", e.getMessage(), e);
                log.warn("Continuing with manual refresh despite cache invalidation failure");
            }

            int refreshedCount = exchangeRateService.refreshAllRates();

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            log.info("Manual refresh completed: {} currency pairs refreshed in {} seconds",
                    refreshedCount, durationSeconds);

            return refreshedCount;

        } catch (Exception e) {
            log.error("Manual refresh failed: {}", e.getMessage(), e);
            throw new RuntimeException("Manual refresh failed: " + e.getMessage(), e);
        }
    }
}
