package com.example.aidemo1.integration.provider;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.exception.UnsupportedCurrencyPairException;
import com.example.aidemo1.integration.client.MockProvider2Client;
import com.example.aidemo1.integration.dto.external.MockProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mock exchange rate provider implementation #2.
 * 
 * <p>This provider fetches mock exchange rates from an internal endpoint
 * for testing and development purposes. It simulates the behavior of real
 * external providers but generates random rates with slightly different
 * characteristics than MockProvider1.</p>
 * 
 * <h2>Configuration</h2>
 * <p>Can be enabled/disabled via the {@code mock.provider2.enabled} property.</p>
 * 
 * <h2>Priority</h2>
 * <p>Mock providers have lower priority (50) than real providers (100) to ensure
 * real data is preferred when available.</p>
 * 
 * @see ExchangeRateProvider
 * @see MockProvider2Client
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mock.provider2.enabled", havingValue = "true", matchIfMissing = true)
public class MockProvider2 implements ExchangeRateProvider {
    
    private static final String PROVIDER_NAME = "Mock Provider 2";
    private static final int PRIORITY = 50;
    
    private final MockProvider2Client client;
    
    public MockProvider2(MockProvider2Client client) {
        this.client = client;
    }
    
    @Override
    @Retryable(
            retryFor = ExternalProviderException.class,
            maxAttempts = 2,
            backoff = @Backoff(delay = 500)
    )
    public ExchangeRate fetchRate(String from, String to) {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Base currency cannot be null or empty");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Target currency cannot be null or empty");
        }
        
        log.info("Fetching exchange rate from {}: {} -> {}", PROVIDER_NAME, from, to);
        
        try {
            MockProviderResponse response = client.getExchangeRate(from, to);
            
            if (response.getRate() == null) {
                throw new UnsupportedCurrencyPairException(from, to, PROVIDER_NAME);
            }
            
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.getTimestamp()),
                    ZoneId.systemDefault()
            );
            
            return ExchangeRate.builder()
                    .baseCurrency(from.toUpperCase())
                    .targetCurrency(to.toUpperCase())
                    .rate(response.getRate())
                    .provider(PROVIDER_NAME)
                    .timestamp(timestamp)
                    .build();
                    
        } catch (UnsupportedCurrencyPairException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch rate from {}: {}", PROVIDER_NAME, e.getMessage());
            throw new ExternalProviderException(
                    "Error fetching rate from " + PROVIDER_NAME + ": " + e.getMessage(),
                    e
            );
        }
    }
    
    @Override
    public boolean supports(String from, String to) {
        // Mock provider supports all currency pairs
        return from != null && !from.isBlank() && to != null && !to.isBlank();
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
