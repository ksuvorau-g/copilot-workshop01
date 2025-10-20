package com.example.aidemo1.service;

import com.example.aidemo1.entity.ExchangeRate;

import java.util.Optional;

/**
 * Service interface for Redis cache operations on exchange rates.
 * Implements cache-aside pattern for exchange rate caching.
 * 
 * <p>This service provides caching operations with the following characteristics:</p>
 * <ul>
 *   <li><strong>Cache key format:</strong> {@code exchange_rate:{from}:{to}} (e.g., {@code exchange_rate:USD:EUR})</li>
 *   <li><strong>TTL:</strong> 3600 seconds (1 hour)</li>
 *   <li><strong>Serialization:</strong> JSON via Jackson</li>
 * </ul>
 */
public interface ExchangeRateCacheService {

    /**
     * Retrieves a cached exchange rate from Redis.
     * 
     * @param from the base currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return Optional containing the cached rate if found, empty otherwise
     */
    Optional<ExchangeRate> getCachedRate(String from, String to);

    /**
     * Caches an exchange rate in Redis with 1-hour TTL.
     * 
     * @param from the base currency code
     * @param to the target currency code
     * @param rate the exchange rate entity to cache
     */
    void cacheRate(String from, String to, ExchangeRate rate);

    /**
     * Invalidates (removes) a specific cached exchange rate.
     * 
     * @param from the base currency code
     * @param to the target currency code
     */
    void invalidateCache(String from, String to);

    /**
     * Invalidates all cached exchange rates using pattern matching.
     * Uses the pattern {@code exchange_rate:*} to remove all rate-related cache entries.
     */
    void invalidateAllCache();
}
