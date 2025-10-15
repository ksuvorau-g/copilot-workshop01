package com.example.aidemo1.cache;

import com.example.aidemo1.entity.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RateCacheService.
 * 
 * These tests verify Redis cache operations work correctly.
 * Requires Redis to be running (via Docker Compose or locally).
 */
@SpringBootTest
class RateCacheServiceTest {

    @Autowired
    private RateCacheService rateCacheService;

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        rateCacheService.invalidateAllRates();
    }

    @Test
    void testCacheAndRetrieveRate() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(new BigDecimal("0.85"))
                .provider("TestProvider")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        rateCacheService.cacheRate(baseCurrency, targetCurrency, rate);
        Optional<ExchangeRate> cachedRate = rateCacheService.getCachedRate(baseCurrency, targetCurrency);

        // Then
        assertThat(cachedRate).isPresent();
        assertThat(cachedRate.get().getBaseCurrency()).isEqualTo(baseCurrency);
        assertThat(cachedRate.get().getTargetCurrency()).isEqualTo(targetCurrency);
        assertThat(cachedRate.get().getRate()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(cachedRate.get().getProvider()).isEqualTo("TestProvider");
    }

    @Test
    void testCacheMissReturnsEmpty() {
        // When
        Optional<ExchangeRate> cachedRate = rateCacheService.getCachedRate("USD", "JPY");

        // Then
        assertThat(cachedRate).isEmpty();
    }

    @Test
    void testInvalidateSpecificRate() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(new BigDecimal("0.85"))
                .provider("TestProvider")
                .timestamp(LocalDateTime.now())
                .build();

        rateCacheService.cacheRate(baseCurrency, targetCurrency, rate);

        // When
        rateCacheService.invalidateRate(baseCurrency, targetCurrency);
        Optional<ExchangeRate> cachedRate = rateCacheService.getCachedRate(baseCurrency, targetCurrency);

        // Then
        assertThat(cachedRate).isEmpty();
    }

    @Test
    void testInvalidateAllRates() {
        // Given
        ExchangeRate rate1 = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.85"))
                .provider("TestProvider")
                .timestamp(LocalDateTime.now())
                .build();

        ExchangeRate rate2 = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("GBP")
                .rate(new BigDecimal("0.75"))
                .provider("TestProvider")
                .timestamp(LocalDateTime.now())
                .build();

        rateCacheService.cacheRate("USD", "EUR", rate1);
        rateCacheService.cacheRate("USD", "GBP", rate2);

        // When
        rateCacheService.invalidateAllRates();

        // Then
        assertThat(rateCacheService.getCachedRate("USD", "EUR")).isEmpty();
        assertThat(rateCacheService.getCachedRate("USD", "GBP")).isEmpty();
    }

    @Test
    void testGenerateCacheKey() {
        // When
        String cacheKey = rateCacheService.generateCacheKey("USD", "EUR");

        // Then
        assertThat(cacheKey).isEqualTo("exchange_rate:USD:EUR");
    }

    @Test
    void testGenerateCacheKeyNormalizesToUpperCase() {
        // When
        String cacheKey = rateCacheService.generateCacheKey("usd", "eur");

        // Then
        assertThat(cacheKey).isEqualTo("exchange_rate:USD:EUR");
    }

    @Test
    void testCacheNullRate() {
        // When
        rateCacheService.cacheRate("USD", "EUR", null);
        Optional<ExchangeRate> cachedRate = rateCacheService.getCachedRate("USD", "EUR");

        // Then
        assertThat(cachedRate).isEmpty();
    }
}
