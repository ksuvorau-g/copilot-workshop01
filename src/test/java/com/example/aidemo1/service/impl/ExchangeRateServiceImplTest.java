package com.example.aidemo1.service.impl;

import com.example.aidemo1.cache.RateCacheService;
import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.CurrencyNotFoundException;
import com.example.aidemo1.exception.ExchangeRateNotFoundException;
import com.example.aidemo1.integration.aggregator.RateAggregatorService;
import com.example.aidemo1.repository.CurrencyRepository;
import com.example.aidemo1.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateServiceImpl Tests")
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private RateCacheService rateCacheService;

    @Mock
    private RateAggregatorService rateAggregatorService;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    private static final String BASE_CURRENCY = "USD";
    private static final String TARGET_CURRENCY = "EUR";
    private static final BigDecimal SAMPLE_RATE = new BigDecimal("0.85");
    private static final BigDecimal SAMPLE_AMOUNT = new BigDecimal("100.00");

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when exchangeRateRepository is null")
        void shouldThrowExceptionWhenExchangeRateRepositoryIsNull() {
            assertThatThrownBy(() -> new ExchangeRateServiceImpl(
                    null, currencyRepository, rateCacheService, rateAggregatorService))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ExchangeRateRepository");
        }

        @Test
        @DisplayName("Should throw NullPointerException when currencyRepository is null")
        void shouldThrowExceptionWhenCurrencyRepositoryIsNull() {
            assertThatThrownBy(() -> new ExchangeRateServiceImpl(
                    exchangeRateRepository, null, rateCacheService, rateAggregatorService))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CurrencyRepository");
        }

        @Test
        @DisplayName("Should throw NullPointerException when rateCacheService is null")
        void shouldThrowExceptionWhenRateCacheServiceIsNull() {
            assertThatThrownBy(() -> new ExchangeRateServiceImpl(
                    exchangeRateRepository, currencyRepository, null, rateAggregatorService))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("RateCacheService");
        }

        @Test
        @DisplayName("Should throw NullPointerException when rateAggregatorService is null")
        void shouldThrowExceptionWhenRateAggregatorServiceIsNull() {
            assertThatThrownBy(() -> new ExchangeRateServiceImpl(
                    exchangeRateRepository, currencyRepository, rateCacheService, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("RateAggregatorService");
        }
    }

    @Nested
    @DisplayName("getExchangeRate Tests")
    class GetExchangeRateTests {

        @BeforeEach
        void setUp() {
            // Mock currency validation to pass by default (lenient for validation tests)
            lenient().when(currencyRepository.existsByCode(BASE_CURRENCY)).thenReturn(true);
            lenient().when(currencyRepository.existsByCode(TARGET_CURRENCY)).thenReturn(true);
        }

        @Test
        @DisplayName("Should return converted amount from cache when available")
        void shouldReturnConvertedAmountFromCache() {
            // Given
            var cachedRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            when(rateCacheService.getCachedRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.of(cachedRate));

            // When
            var result = exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT);

            // Then
            var expectedAmount = SAMPLE_AMOUNT.multiply(SAMPLE_RATE)
                    .setScale(6, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expectedAmount);

            // Verify cache was checked
            verify(rateCacheService).getCachedRate(BASE_CURRENCY, TARGET_CURRENCY);
            // Verify database was NOT queried (cache hit)
            verify(exchangeRateRepository, never()).findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(anyString(), anyString());
            // Verify providers were NOT called (cache hit)
            verify(rateAggregatorService, never()).fetchAndAggregate(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return converted amount from database when cache miss but recent rate exists")
        void shouldReturnConvertedAmountFromDatabase() {
            // Given
            when(rateCacheService.getCachedRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.empty());

            var recentRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            recentRate.setTimestamp(LocalDateTime.now().minusMinutes(30)); // Recent (within 1 hour)
            when(exchangeRateRepository.findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.of(recentRate));

            // When
            var result = exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT);

            // Then
            var expectedAmount = SAMPLE_AMOUNT.multiply(SAMPLE_RATE)
                    .setScale(6, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expectedAmount);

            // Verify cache was checked
            verify(rateCacheService).getCachedRate(BASE_CURRENCY, TARGET_CURRENCY);
            // Verify database was queried
            verify(exchangeRateRepository).findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY);
            // Verify rate was cached after database retrieval
            verify(rateCacheService).cacheRate(BASE_CURRENCY, TARGET_CURRENCY, recentRate);
            // Verify providers were NOT called (database hit)
            verify(rateAggregatorService, never()).fetchAndAggregate(anyString(), anyString());
        }

        @Test
        @DisplayName("Should fetch from providers when cache and database miss")
        void shouldFetchFromProvidersWhenCacheAndDatabaseMiss() {
            // Given
            when(rateCacheService.getCachedRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.empty());
            when(exchangeRateRepository.findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.empty());

            var freshRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            when(rateAggregatorService.fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(freshRate);

            // When
            var result = exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT);

            // Then
            var expectedAmount = SAMPLE_AMOUNT.multiply(SAMPLE_RATE)
                    .setScale(6, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expectedAmount);

            // Verify full lookup chain
            verify(rateCacheService).getCachedRate(BASE_CURRENCY, TARGET_CURRENCY);
            verify(exchangeRateRepository).findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY);
            verify(rateAggregatorService).fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY);
            verify(rateCacheService).cacheRate(BASE_CURRENCY, TARGET_CURRENCY, freshRate);
        }

        @Test
        @DisplayName("Should fetch from providers when database rate is stale")
        void shouldFetchFromProvidersWhenDatabaseRateIsStale() {
            // Given
            when(rateCacheService.getCachedRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.empty());

            var staleRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            staleRate.setTimestamp(LocalDateTime.now().minusHours(2)); // Stale (over 1 hour)
            when(exchangeRateRepository.findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.of(staleRate));

            var freshRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            when(rateAggregatorService.fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(freshRate);

            // When
            var result = exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT);

            // Then
            var expectedAmount = SAMPLE_AMOUNT.multiply(SAMPLE_RATE)
                    .setScale(6, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expectedAmount);

            // Verify providers were called because database rate was stale
            verify(rateAggregatorService).fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when base currency is null")
        void shouldThrowExceptionWhenBaseCurrencyIsNull() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(null, TARGET_CURRENCY, SAMPLE_AMOUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Base currency code must not be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when target currency is null")
        void shouldThrowExceptionWhenTargetCurrencyIsNull() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(BASE_CURRENCY, null, SAMPLE_AMOUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target currency code must not be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(
                    BASE_CURRENCY, TARGET_CURRENCY, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be greater than zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(
                    BASE_CURRENCY, TARGET_CURRENCY, new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be greater than zero");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency codes are same")
        void shouldThrowExceptionWhenCurrencyCodesAreSame() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(BASE_CURRENCY, BASE_CURRENCY, SAMPLE_AMOUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Base and target currencies must be different");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency code length is invalid")
        void shouldThrowExceptionWhenCurrencyCodeLengthIsInvalid() {
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate("US", TARGET_CURRENCY, SAMPLE_AMOUNT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency codes must be exactly 3 characters");
        }

        @Test
        @DisplayName("Should throw CurrencyNotFoundException when base currency doesn't exist")
        void shouldThrowExceptionWhenBaseCurrencyDoesNotExist() {
            // Given
            when(currencyRepository.existsByCode(BASE_CURRENCY)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT))
                    .isInstanceOf(CurrencyNotFoundException.class)
                    .hasMessageContaining(BASE_CURRENCY);
        }

        @Test
        @DisplayName("Should throw CurrencyNotFoundException when target currency doesn't exist")
        void shouldThrowExceptionWhenTargetCurrencyDoesNotExist() {
            // Given
            when(currencyRepository.existsByCode(TARGET_CURRENCY)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_AMOUNT))
                    .isInstanceOf(CurrencyNotFoundException.class)
                    .hasMessageContaining(TARGET_CURRENCY);
        }
    }

    @Nested
    @DisplayName("refreshAllRates Tests")
    class RefreshAllRatesTests {

        @Test
        @DisplayName("Should refresh all currency pairs and return success count")
        void shouldRefreshAllCurrencyPairs() {
            // Given
            var usd = createCurrency("USD");
            var eur = createCurrency("EUR");
            var gbp = createCurrency("GBP");
            when(currencyRepository.findAll()).thenReturn(List.of(usd, eur, gbp));

            // Mock aggregator responses
            when(rateAggregatorService.fetchAndAggregateMultiple(eq("USD"), anyList()))
                    .thenReturn(Map.of(
                            "EUR", createExchangeRate("USD", "EUR", new BigDecimal("0.85")),
                            "GBP", createExchangeRate("USD", "GBP", new BigDecimal("0.73"))
                    ));
            when(rateAggregatorService.fetchAndAggregateMultiple(eq("EUR"), anyList()))
                    .thenReturn(Map.of(
                            "USD", createExchangeRate("EUR", "USD", new BigDecimal("1.18")),
                            "GBP", createExchangeRate("EUR", "GBP", new BigDecimal("0.86"))
                    ));
            when(rateAggregatorService.fetchAndAggregateMultiple(eq("GBP"), anyList()))
                    .thenReturn(Map.of(
                            "USD", createExchangeRate("GBP", "USD", new BigDecimal("1.37")),
                            "EUR", createExchangeRate("GBP", "EUR", new BigDecimal("1.16"))
                    ));

            // When
            var result = exchangeRateService.refreshAllRates();

            // Then
            assertThat(result).isEqualTo(6); // 3 currencies * 2 targets each = 6 pairs

            // Verify aggregator was called for each currency
            verify(rateAggregatorService).fetchAndAggregateMultiple(eq("USD"), anyList());
            verify(rateAggregatorService).fetchAndAggregateMultiple(eq("EUR"), anyList());
            verify(rateAggregatorService).fetchAndAggregateMultiple(eq("GBP"), anyList());

            // Verify caching was performed (6 pairs)
            verify(rateCacheService, times(6)).cacheRate(anyString(), anyString(), any(ExchangeRate.class));
        }

        @Test
        @DisplayName("Should return zero when no currencies exist")
        void shouldReturnZeroWhenNoCurrenciesExist() {
            // Given
            when(currencyRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            var result = exchangeRateService.refreshAllRates();

            // Then
            assertThat(result).isZero();
            verify(rateAggregatorService, never()).fetchAndAggregateMultiple(anyString(), anyList());
            verify(rateCacheService, never()).cacheRate(anyString(), anyString(), any(ExchangeRate.class));
        }

        @Test
        @DisplayName("Should handle partial failures gracefully")
        void shouldHandlePartialFailuresGracefully() {
            // Given
            var usd = createCurrency("USD");
            var eur = createCurrency("EUR");
            when(currencyRepository.findAll()).thenReturn(List.of(usd, eur));

            // Mock aggregator: USD succeeds, EUR partially fails
            when(rateAggregatorService.fetchAndAggregateMultiple(eq("USD"), anyList()))
                    .thenReturn(Map.of("EUR", createExchangeRate("USD", "EUR", new BigDecimal("0.85"))));
            when(rateAggregatorService.fetchAndAggregateMultiple(eq("EUR"), anyList()))
                    .thenReturn(Collections.emptyMap()); // All providers failed

            // When
            var result = exchangeRateService.refreshAllRates();

            // Then
            assertThat(result).isEqualTo(1); // Only 1 successful pair
            verify(rateCacheService, times(1)).cacheRate(anyString(), anyString(), any(ExchangeRate.class));
        }
    }

    @Nested
    @DisplayName("getBestRate Tests")
    class GetBestRateTests {

        @Test
        @DisplayName("Should return best rate from database")
        void shouldReturnBestRateFromDatabase() {
            // Given
            var expectedRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            when(exchangeRateRepository.findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.of(expectedRate));

            // When
            var result = exchangeRateService.getBestRate(BASE_CURRENCY, TARGET_CURRENCY);

            // Then
            assertThat(result).isEqualTo(expectedRate);
            assertThat(result.getRate()).isEqualByComparingTo(SAMPLE_RATE);

            // Verify only database was queried
            verify(exchangeRateRepository).findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY);
            // Verify cache was NOT checked
            verify(rateCacheService, never()).getCachedRate(anyString(), anyString());
            // Verify providers were NOT called
            verify(rateAggregatorService, never()).fetchAndAggregate(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw ExchangeRateNotFoundException when no rate found in database")
        void shouldThrowExceptionWhenNoRateFoundInDatabase() {
            // Given
            when(exchangeRateRepository.findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.getBestRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .isInstanceOf(ExchangeRateNotFoundException.class)
                    .hasMessageContaining(BASE_CURRENCY)
                    .hasMessageContaining(TARGET_CURRENCY);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency codes are invalid")
        void shouldThrowExceptionWhenCurrencyCodesAreInvalid() {
            assertThatThrownBy(() -> exchangeRateService.getBestRate("US", TARGET_CURRENCY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency codes must be exactly 3 characters");
        }
    }

    @Nested
    @DisplayName("fetchFreshRate Tests")
    class FetchFreshRateTests {

        @BeforeEach
        void setUp() {
            // Mock currency validation to pass by default (lenient for validation tests)
            lenient().when(currencyRepository.existsByCode(BASE_CURRENCY)).thenReturn(true);
            lenient().when(currencyRepository.existsByCode(TARGET_CURRENCY)).thenReturn(true);
        }

        @Test
        @DisplayName("Should fetch fresh rate from providers and update cache")
        void shouldFetchFreshRateAndUpdateCache() {
            // Given
            var freshRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, SAMPLE_RATE);
            when(rateAggregatorService.fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY))
                    .thenReturn(freshRate);

            // When
            var result = exchangeRateService.fetchFreshRate(BASE_CURRENCY, TARGET_CURRENCY);

            // Then
            assertThat(result).isEqualTo(freshRate);
            assertThat(result.getRate()).isEqualByComparingTo(SAMPLE_RATE);

            // Verify providers were called
            verify(rateAggregatorService).fetchAndAggregate(BASE_CURRENCY, TARGET_CURRENCY);
            // Verify cache was updated
            verify(rateCacheService).cacheRate(BASE_CURRENCY, TARGET_CURRENCY, freshRate);
            // Verify cache was NOT checked
            verify(rateCacheService, never()).getCachedRate(anyString(), anyString());
            // Verify database was NOT queried
            verify(exchangeRateRepository, never()).findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
                    anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw CurrencyNotFoundException when base currency doesn't exist")
        void shouldThrowExceptionWhenBaseCurrencyDoesNotExist() {
            // Given
            when(currencyRepository.existsByCode(BASE_CURRENCY)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> exchangeRateService.fetchFreshRate(BASE_CURRENCY, TARGET_CURRENCY))
                    .isInstanceOf(CurrencyNotFoundException.class)
                    .hasMessageContaining(BASE_CURRENCY);

            verify(rateAggregatorService, never()).fetchAndAggregate(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency codes are invalid")
        void shouldThrowExceptionWhenCurrencyCodesAreInvalid() {
            assertThatThrownBy(() -> exchangeRateService.fetchFreshRate(BASE_CURRENCY, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target currency code must not be null or empty");
        }
    }

    // Helper methods

    private ExchangeRate createExchangeRate(String from, String to, BigDecimal rate) {
        var exchangeRate = new ExchangeRate();
        exchangeRate.setBaseCurrency(from);
        exchangeRate.setTargetCurrency(to);
        exchangeRate.setRate(rate);
        exchangeRate.setProvider("TestProvider");
        exchangeRate.setTimestamp(LocalDateTime.now());
        return exchangeRate;
    }

    private Currency createCurrency(String code) {
        var currency = new Currency();
        currency.setCode(code);
        currency.setName(code + " Name");
        return currency;
    }
}
