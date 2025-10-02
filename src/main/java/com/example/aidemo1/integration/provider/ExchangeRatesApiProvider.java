package com.example.aidemo1.integration.provider;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.exception.UnsupportedCurrencyPairException;
import com.example.aidemo1.integration.client.ExchangeRatesApiClient;
import com.example.aidemo1.integration.dto.external.ExchangeRatesApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Exchange rate provider implementation for ExchangeRatesAPI.io.
 * 
 * <p>Fetches real exchange rates from ExchangeRatesAPI.io with automatic retry logic
 * for transient failures.</p>
 * 
 * @see ExchangeRateProvider
 */
@Slf4j
@Component
public class ExchangeRatesApiProvider implements ExchangeRateProvider {
    
    private static final String PROVIDER_NAME = "ExchangeRatesAPI";
    private static final int PRIORITY = 100;
    
    private final ExchangeRatesApiClient client;
    
    public ExchangeRatesApiProvider(ExchangeRatesApiClient client) {
        this.client = client;
    }
    
    @Override
    @Retryable(
            retryFor = ExternalProviderException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
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
            ExchangeRatesApiResponse response = client.getExchangeRate(from, to);
            
            if (response.getRates() == null || response.getRates().isEmpty()) {
                throw new UnsupportedCurrencyPairException(from, to, PROVIDER_NAME);
            }
            
            BigDecimal rate = response.getRates().get(to);
            if (rate == null) {
                throw new UnsupportedCurrencyPairException(from, to, PROVIDER_NAME);
            }
            
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(response.getTimestamp()),
                    ZoneId.systemDefault()
            );
            
            return ExchangeRate.builder()
                    .baseCurrency(from)
                    .targetCurrency(to)
                    .rate(rate)
                    .provider(PROVIDER_NAME)
                    .timestamp(timestamp)
                    .build();
                    
        } catch (UnsupportedCurrencyPairException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching rate from {}: {}", PROVIDER_NAME, e.getMessage());
            throw new ExternalProviderException("Failed to fetch rate from " + PROVIDER_NAME, e);
        }
    }
    
    @Override
    public boolean supports(String from, String to) {
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
