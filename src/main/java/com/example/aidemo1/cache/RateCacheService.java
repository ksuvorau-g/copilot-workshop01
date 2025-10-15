package com.example.aidemo1.cache;

import com.example.aidemo1.entity.ExchangeRate;

import java.util.Optional;

/**
 * Service interface for caching exchange rate data in Redis.
 * 
 * Provides operations to store, retrieve, and invalidate cached exchange rates.
 * Cache keys follow the pattern: exchange_rate:{baseCurrency}:{targetCurrency}
 * 
 * TTL (Time To Live) is set to 1 hour, aligned with the scheduled rate refresh.
 */
public interface RateCacheService {

    /**
     * Store an exchange rate in the cache.
     * 
     * Caches the best available rate for a currency pair with 1 hour TTL.
     * The cached value will be automatically evicted after expiration.
     * 
     * @param baseCurrency Base currency code (e.g., "USD")
     * @param targetCurrency Target currency code (e.g., "EUR")
     * @param rate Exchange rate to cache
     */
    void cacheRate(String baseCurrency, String targetCurrency, ExchangeRate rate);

    /**
     * Retrieve a cached exchange rate.
     * 
     * Returns the cached rate if available and not expired.
     * 
     * @param baseCurrency Base currency code (e.g., "USD")
     * @param targetCurrency Target currency code (e.g., "EUR")
     * @return Optional containing the cached rate, or empty if not found or expired
     */
    Optional<ExchangeRate> getCachedRate(String baseCurrency, String targetCurrency);

    /**
     * Invalidate a specific cached rate.
     * 
     * Removes the cached rate for a specific currency pair.
     * Used when rates are manually refreshed or updated.
     * 
     * @param baseCurrency Base currency code (e.g., "USD")
     * @param targetCurrency Target currency code (e.g., "EUR")
     */
    void invalidateRate(String baseCurrency, String targetCurrency);

    /**
     * Invalidate all cached rates.
     * 
     * Clears all cached exchange rates from Redis.
     * Used during scheduled refresh to ensure fresh data is fetched.
     */
    void invalidateAllRates();

    /**
     * Generate cache key for a currency pair.
     * 
     * Creates a consistent key format for Redis storage.
     * Pattern: exchange_rate:{baseCurrency}:{targetCurrency}
     * 
     * @param baseCurrency Base currency code
     * @param targetCurrency Target currency code
     * @return Cache key string
     */
    String generateCacheKey(String baseCurrency, String targetCurrency);
}
