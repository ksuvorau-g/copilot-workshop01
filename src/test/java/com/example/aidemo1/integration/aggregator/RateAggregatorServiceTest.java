package com.example.aidemo1.integration.aggregator;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.provider.ExchangeRateProvider;
import com.example.aidemo1.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateAggregatorService.
 * Tests rate aggregation logic, provider selection, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateAggregatorService Tests")
class RateAggregatorServiceTest {
    
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    
    @Mock
    private ExchangeRateProvider provider1;
    
    @Mock
    private ExchangeRateProvider provider2;
    
    @Mock
    private ExchangeRateProvider provider3;
    
    @Captor
    private ArgumentCaptor<List<ExchangeRate>> rateListCaptor;
    
    private RateAggregatorService aggregatorService;
    
    @BeforeEach
    void setUp() {
        // Setup provider1 with highest priority
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.getPriority()).thenReturn(100);
        
        // Setup provider2 with medium priority
        when(provider2.getProviderName()).thenReturn("Provider2");
        when(provider2.getPriority()).thenReturn(80);
        
        // Setup provider3 with lowest priority
        when(provider3.getProviderName()).thenReturn("Provider3");
        when(provider3.getPriority()).thenReturn(50);
        
        List<ExchangeRateProvider> providers = Arrays.asList(provider1, provider2, provider3);
        aggregatorService = new RateAggregatorService(providers, exchangeRateRepository);
    }
    
    @Test
    @DisplayName("Should successfully aggregate rates from multiple providers")
    void shouldAggregateRatesFromMultipleProviders() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        when(provider3.supports(from, to)).thenReturn(false);
        
        ExchangeRate rate1 = createRate(from, to, "1.10", "Provider1");
        ExchangeRate rate2 = createRate(from, to, "1.08", "Provider2");
        
        when(provider1.fetchRate(from, to)).thenReturn(rate1);
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ExchangeRate result = aggregatorService.fetchAndAggregate(from, to);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRate()).isEqualTo(new BigDecimal("1.08")); // Best rate
        assertThat(result.getProvider()).isEqualTo("Provider2");
        
        verify(provider1).fetchRate(from, to);
        verify(provider2).fetchRate(from, to);
        verify(provider3, never()).fetchRate(any(), any());
        verify(exchangeRateRepository).saveAll(rateListCaptor.capture());
        
        List<ExchangeRate> savedRates = rateListCaptor.getValue();
        assertThat(savedRates).hasSize(2);
    }
    
    @Test
    @DisplayName("Should select best rate based on lowest value")
    void shouldSelectBestRateBasedOnLowestValue() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        when(provider3.supports(from, to)).thenReturn(true);
        
        ExchangeRate rate1 = createRate(from, to, "1.15", "Provider1");
        ExchangeRate rate2 = createRate(from, to, "1.05", "Provider2"); // Best
        ExchangeRate rate3 = createRate(from, to, "1.20", "Provider3");
        
        when(provider1.fetchRate(from, to)).thenReturn(rate1);
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        when(provider3.fetchRate(from, to)).thenReturn(rate3);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ExchangeRate result = aggregatorService.fetchAndAggregate(from, to);
        
        // Then
        assertThat(result.getRate()).isEqualTo(new BigDecimal("1.05"));
        assertThat(result.getProvider()).isEqualTo("Provider2");
    }
    
    @Test
    @DisplayName("Should prefer higher priority provider when rates are equal")
    void shouldPreferHigherPriorityProviderWhenRatesAreEqual() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        
        ExchangeRate rate1 = createRate(from, to, "1.10", "Provider1"); // Priority 100
        ExchangeRate rate2 = createRate(from, to, "1.10", "Provider2"); // Priority 80
        
        when(provider1.fetchRate(from, to)).thenReturn(rate1);
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ExchangeRate result = aggregatorService.fetchAndAggregate(from, to);
        
        // Then
        assertThat(result.getRate()).isEqualTo(new BigDecimal("1.10"));
        assertThat(result.getProvider()).isEqualTo("Provider1"); // Higher priority
    }
    
    @Test
    @DisplayName("Should handle provider failure gracefully and continue with others")
    void shouldHandleProviderFailureGracefully() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        
        when(provider1.fetchRate(from, to)).thenThrow(new ExternalProviderException("Provider1 failed"));
        
        ExchangeRate rate2 = createRate(from, to, "1.08", "Provider2");
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ExchangeRate result = aggregatorService.fetchAndAggregate(from, to);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo("Provider2");
        
        verify(exchangeRateRepository).saveAll(rateListCaptor.capture());
        assertThat(rateListCaptor.getValue()).hasSize(1);
    }
    
    @Test
    @DisplayName("Should throw exception when all providers fail")
    void shouldThrowExceptionWhenAllProvidersFail() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        
        when(provider1.fetchRate(from, to)).thenThrow(new ExternalProviderException("Provider1 failed"));
        when(provider2.fetchRate(from, to)).thenThrow(new ExternalProviderException("Provider2 failed"));
        
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate(from, to))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("All providers failed");
        
        verify(exchangeRateRepository, never()).saveAll(anyList());
    }
    
    @Test
    @DisplayName("Should throw exception when no providers support currency pair")
    void shouldThrowExceptionWhenNoProvidersSupportCurrencyPair() {
        // Given
        String from = "XXX";
        String to = "YYY";
        
        when(provider1.supports(from, to)).thenReturn(false);
        when(provider2.supports(from, to)).thenReturn(false);
        when(provider3.supports(from, to)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate(from, to))
                .isInstanceOf(ExternalProviderException.class)
                .hasMessageContaining("No providers support currency pair");
        
        verify(provider1, never()).fetchRate(any(), any());
        verify(exchangeRateRepository, never()).saveAll(anyList());
    }
    
    @Test
    @DisplayName("Should validate currency codes - null base currency")
    void shouldValidateCurrencyCodesNullBase() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate(null, "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base currency cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should validate currency codes - empty base currency")
    void shouldValidateCurrencyCodesEmptyBase() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate("", "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base currency cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should validate currency codes - null target currency")
    void shouldValidateCurrencyCodesNullTarget() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate("USD", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target currency cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should validate currency codes - invalid length")
    void shouldValidateCurrencyCodesInvalidLength() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate("US", "EUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be exactly 3 characters");
    }
    
    @Test
    @DisplayName("Should validate currency codes - same base and target")
    void shouldValidateCurrencyCodesSameBaseAndTarget() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregate("USD", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base and target currencies must be different");
    }
    
    @Test
    @DisplayName("Should fetch and aggregate multiple currency pairs")
    void shouldFetchAndAggregateMultipleCurrencyPairs() {
        // Given
        String from = "USD";
        List<String> targets = Arrays.asList("EUR", "GBP");
        
        when(provider1.supports(from, "EUR")).thenReturn(true);
        when(provider1.supports(from, "GBP")).thenReturn(true);
        
        ExchangeRate rateEur = createRate(from, "EUR", "1.10", "Provider1");
        ExchangeRate rateGbp = createRate(from, "GBP", "0.85", "Provider1");
        
        when(provider1.fetchRate(from, "EUR")).thenReturn(rateEur);
        when(provider1.fetchRate(from, "GBP")).thenReturn(rateGbp);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Map<String, ExchangeRate> results = aggregatorService.fetchAndAggregateMultiple(from, targets);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results).containsKeys("EUR", "GBP");
        assertThat(results.get("EUR").getRate()).isEqualTo(new BigDecimal("1.10"));
        assertThat(results.get("GBP").getRate()).isEqualTo(new BigDecimal("0.85"));
    }
    
    @Test
    @DisplayName("Should handle partial failures in multiple currency pairs")
    void shouldHandlePartialFailuresInMultipleCurrencyPairs() {
        // Given
        String from = "USD";
        List<String> targets = Arrays.asList("EUR", "GBP", "JPY");
        
        // EUR - only provider1 supports
        when(provider1.supports(from, "EUR")).thenReturn(true);
        when(provider2.supports(from, "EUR")).thenReturn(false);
        when(provider3.supports(from, "EUR")).thenReturn(false);
        
        // GBP - only provider1 supports
        when(provider1.supports(from, "GBP")).thenReturn(true);
        when(provider2.supports(from, "GBP")).thenReturn(false);
        when(provider3.supports(from, "GBP")).thenReturn(false);
        
        // JPY - no providers support
        when(provider1.supports(from, "JPY")).thenReturn(false);
        when(provider2.supports(from, "JPY")).thenReturn(false);
        when(provider3.supports(from, "JPY")).thenReturn(false);
        
        ExchangeRate rateEur = createRate(from, "EUR", "1.10", "Provider1");
        ExchangeRate rateGbp = createRate(from, "GBP", "0.85", "Provider1");
        
        when(provider1.fetchRate(from, "EUR")).thenReturn(rateEur);
        when(provider1.fetchRate(from, "GBP")).thenReturn(rateGbp);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Map<String, ExchangeRate> results = aggregatorService.fetchAndAggregateMultiple(from, targets);
        
        // Then
        assertThat(results).hasSize(2); // Only EUR and GBP succeeded
        assertThat(results).containsKeys("EUR", "GBP");
        assertThat(results).doesNotContainKey("JPY");
    }
    
    @Test
    @DisplayName("Should validate multiple targets - null list")
    void shouldValidateMultipleTargetsNullList() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregateMultiple("USD", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target currencies list cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should validate multiple targets - empty list")
    void shouldValidateMultipleTargetsEmptyList() {
        // When/Then
        assertThatThrownBy(() -> aggregatorService.fetchAndAggregateMultiple("USD", Arrays.asList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target currencies list cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should fetch from all providers without persisting")
    void shouldFetchFromAllProvidersWithoutPersisting() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        when(provider3.supports(from, to)).thenReturn(false);
        
        ExchangeRate rate1 = createRate(from, to, "1.10", "Provider1");
        ExchangeRate rate2 = createRate(from, to, "1.08", "Provider2");
        
        when(provider1.fetchRate(from, to)).thenReturn(rate1);
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        // When
        List<ExchangeRate> results = aggregatorService.fetchFromAllProviders(from, to);
        
        // Then
        assertThat(results).hasSize(2);
        verify(exchangeRateRepository, never()).saveAll(anyList());
    }
    
    @Test
    @DisplayName("Should handle provider failures in fetchFromAllProviders")
    void shouldHandleProviderFailuresInFetchFromAllProviders() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        
        when(provider1.fetchRate(from, to)).thenThrow(new ExternalProviderException("Failed"));
        ExchangeRate rate2 = createRate(from, to, "1.08", "Provider2");
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        // When
        List<ExchangeRate> results = aggregatorService.fetchFromAllProviders(from, to);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProvider()).isEqualTo("Provider2");
    }
    
    @Test
    @DisplayName("Should return provider count")
    void shouldReturnProviderCount() {
        // When
        int count = aggregatorService.getProviderCount();
        
        // Then
        assertThat(count).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should return provider names")
    void shouldReturnProviderNames() {
        // When
        List<String> names = aggregatorService.getProviderNames();
        
        // Then
        assertThat(names).hasSize(3);
        assertThat(names).containsExactlyInAnyOrder("Provider1", "Provider2", "Provider3");
    }
    
    @Test
    @DisplayName("Should save all fetched rates to database")
    void shouldSaveAllFetchedRatesToDatabase() {
        // Given
        String from = "USD";
        String to = "EUR";
        
        when(provider1.supports(from, to)).thenReturn(true);
        when(provider2.supports(from, to)).thenReturn(true);
        
        ExchangeRate rate1 = createRate(from, to, "1.10", "Provider1");
        ExchangeRate rate2 = createRate(from, to, "1.08", "Provider2");
        
        when(provider1.fetchRate(from, to)).thenReturn(rate1);
        when(provider2.fetchRate(from, to)).thenReturn(rate2);
        
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        aggregatorService.fetchAndAggregate(from, to);
        
        // Then
        verify(exchangeRateRepository).saveAll(rateListCaptor.capture());
        List<ExchangeRate> savedRates = rateListCaptor.getValue();
        assertThat(savedRates).hasSize(2);
        assertThat(savedRates).extracting(ExchangeRate::getProvider)
                .containsExactlyInAnyOrder("Provider1", "Provider2");
    }
    
    // Helper method to create ExchangeRate instances
    private ExchangeRate createRate(String from, String to, String rateValue, String provider) {
        return ExchangeRate.builder()
                .baseCurrency(from)
                .targetCurrency(to)
                .rate(new BigDecimal(rateValue))
                .provider(provider)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
