package com.example.aidemo1.integration.client;

import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.dto.external.ExchangeRatesApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for ExchangeRatesAPI.io communication.
 * 
 * <p>Handles all HTTP requests to ExchangeRatesAPI.io and converts responses
 * to internal DTOs.</p>
 */
@Slf4j
@Component
public class ExchangeRatesApiClient {
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    
    public ExchangeRatesApiClient(
            RestTemplate restTemplate,
            @Value("${exchange.provider.exchangeratesapi.base-url}") String baseUrl,
            @Value("${exchange.provider.exchangeratesapi.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }
    
    /**
     * Fetches exchange rate from ExchangeRatesAPI.io.
     * 
     * @param from base currency code
     * @param to target currency code
     * @return ExchangeRatesApiResponse containing rate information
     * @throws ExternalProviderException if API call fails
     */
    public ExchangeRatesApiResponse getExchangeRate(String from, String to) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/latest")
                .queryParam("access_key", apiKey)
                .queryParam("base", from)
                .queryParam("symbols", to)
                .build()
                .toUriString();
        
        log.debug("Calling ExchangeRatesAPI.io: {} -> {}", from, to);
        
        try {
            ExchangeRatesApiResponse response = restTemplate.getForObject(url, ExchangeRatesApiResponse.class);
            
            if (response == null) {
                throw new ExternalProviderException("ExchangeRatesAPI.io returned null response");
            }
            
            if (!response.isSuccess()) {
                String errorMsg = response.getError() != null 
                        ? response.getError().getMessage() 
                        : "Unknown error";
                throw new ExternalProviderException("ExchangeRatesAPI.io API error: " + errorMsg);
            }
            
            log.debug("Successfully fetched rate from ExchangeRatesAPI.io: {} -> {} = {}", 
                    from, to, response.getRates().get(to));
            
            return response;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                throw new ExternalProviderException("ExchangeRatesAPI.io authentication failed. Check API key.", e);
            } else if (e.getStatusCode().value() == 429) {
                throw new ExternalProviderException("ExchangeRatesAPI.io rate limit exceeded.", e);
            }
            throw new ExternalProviderException("ExchangeRatesAPI.io client error: " + e.getMessage(), e);
            
        } catch (HttpServerErrorException e) {
            throw new ExternalProviderException("ExchangeRatesAPI.io server error: " + e.getMessage(), e);
            
        } catch (ResourceAccessException e) {
            throw new ExternalProviderException("Network error calling ExchangeRatesAPI.io: " + e.getMessage(), e);
            
        } catch (Exception e) {
            if (e instanceof ExternalProviderException) {
                throw e;
            }
            throw new ExternalProviderException("Unexpected error calling ExchangeRatesAPI.io: " + e.getMessage(), e);
        }
    }
}
