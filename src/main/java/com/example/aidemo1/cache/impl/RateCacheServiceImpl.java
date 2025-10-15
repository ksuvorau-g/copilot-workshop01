package com.example.aidemo1.cache.impl;

import com.example.aidemo1.cache.RateCacheService;
import com.example.aidemo1.entity.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-based implementation of exchange rate caching service.
 * 
 * Uses RedisTemplate for cache operations with:
 * - String keys following pattern: exchange_rate:{base}:{target}
 * - JSON serialized ExchangeRate values
 * - 1 hour TTL (Time To Live)
 * - Pattern-based cache invalidation
 */
@Service
@Slf4j
public class RateCacheServiceImpl implements RateCacheService {

    private static final String CACHE_KEY_PREFIX = "exchange_rate";
    private static final String CACHE_KEY_SEPARATOR = ":";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Constructor injection for Redis template.
     * 
     * @param redisTemplate Configured RedisTemplate with JSON serialization
     */
    public RateCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cacheRate(String baseCurrency, String targetCurrency, ExchangeRate rate) {
        if (rate == null) {
            log.warn("Attempted to cache null rate for {}:{}", baseCurrency, targetCurrency);
            return;
        }

        String cacheKey = generateCacheKey(baseCurrency, targetCurrency);
        
        try {
            redisTemplate.opsForValue().set(cacheKey, rate, CACHE_TTL);
            log.debug("Cached rate for {} to {} with TTL of {} seconds", 
                     baseCurrency, targetCurrency, CACHE_TTL.getSeconds());
        } catch (Exception e) {
            // Log but don't fail - application should work even if Redis is unavailable
            log.error("Failed to cache rate for {}:{}: {}", baseCurrency, targetCurrency, e.getMessage());
        }
    }

    @Override
    public Optional<ExchangeRate> getCachedRate(String baseCurrency, String targetCurrency) {
        String cacheKey = generateCacheKey(baseCurrency, targetCurrency);
        
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedValue == null) {
                log.debug("Cache miss for {}:{}", baseCurrency, targetCurrency);
                return Optional.empty();
            }
            
            if (cachedValue instanceof ExchangeRate exchangeRate) {
                log.debug("Cache hit for {}:{}", baseCurrency, targetCurrency);
                return Optional.of(exchangeRate);
            } else {
                log.warn("Cached value for {}:{} is not an ExchangeRate instance: {}", 
                        baseCurrency, targetCurrency, cachedValue.getClass().getName());
                // Invalid cache entry - remove it
                invalidateRate(baseCurrency, targetCurrency);
                return Optional.empty();
            }
        } catch (Exception e) {
            // Log but return empty - fallback to database if Redis fails
            log.error("Failed to retrieve cached rate for {}:{}: {}", 
                     baseCurrency, targetCurrency, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void invalidateRate(String baseCurrency, String targetCurrency) {
        String cacheKey = generateCacheKey(baseCurrency, targetCurrency);
        
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Invalidated cache for {}:{}", baseCurrency, targetCurrency);
            } else {
                log.debug("No cache entry found to invalidate for {}:{}", baseCurrency, targetCurrency);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache for {}:{}: {}", 
                     baseCurrency, targetCurrency, e.getMessage());
        }
    }

    @Override
    public void invalidateAllRates() {
        String pattern = CACHE_KEY_PREFIX + CACHE_KEY_SEPARATOR + "*";
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                log.debug("No cache entries found to invalidate");
                return;
            }
            
            Long deletedCount = redisTemplate.delete(keys);
            log.info("Invalidated {} cached exchange rates", deletedCount != null ? deletedCount : 0);
        } catch (Exception e) {
            log.error("Failed to invalidate all cached rates: {}", e.getMessage());
        }
    }

    @Override
    public String generateCacheKey(String baseCurrency, String targetCurrency) {
        // Normalize to uppercase for consistent cache keys
        String normalizedBase = baseCurrency != null ? baseCurrency.toUpperCase() : "";
        String normalizedTarget = targetCurrency != null ? targetCurrency.toUpperCase() : "";
        
        return CACHE_KEY_PREFIX + CACHE_KEY_SEPARATOR + normalizedBase + CACHE_KEY_SEPARATOR + normalizedTarget;
    }
}
