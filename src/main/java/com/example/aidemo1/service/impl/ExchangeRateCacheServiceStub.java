package com.example.aidemo1.service.impl;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.service.ExchangeRateCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Stub implementation of ExchangeRateCacheService.
 * 
 * <p>This is a temporary no-op implementation that logs cache operations
 * but does not actually perform any caching. This stub allows the application
 * to compile and run while the full Redis-based implementation is being developed.</p>
 * 
 * <p><strong>Note:</strong> This class will be replaced by a full Redis implementation
 * in issue #20. Do not use this class for actual caching functionality.</p>
 * 
 * @see ExchangeRateCacheService
 */
@Slf4j
@Service
public class ExchangeRateCacheServiceStub implements ExchangeRateCacheService {

    @Override
    public Optional<ExchangeRate> getCachedRate(String from, String to) {
        log.debug("Cache stub: getCachedRate called for {} -> {} (not implemented, returning empty)", from, to);
        return Optional.empty();
    }

    @Override
    public void cacheRate(String from, String to, ExchangeRate rate) {
        log.debug("Cache stub: cacheRate called for {} -> {} (not implemented, no-op)", from, to);
        // No-op: stub does not cache
    }

    @Override
    public void invalidateCache(String from, String to) {
        log.debug("Cache stub: invalidateCache called for {} -> {} (not implemented, no-op)", from, to);
        // No-op: stub does not invalidate
    }

    @Override
    public void invalidateAllCache() {
        log.debug("Cache stub: invalidateAllCache called (not implemented, no-op)");
        // No-op: stub does not invalidate
    }
}
