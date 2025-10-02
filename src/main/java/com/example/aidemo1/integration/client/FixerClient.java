package com.example.aidemo1.integration.client;

import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.dto.external.FixerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP client for Fixer.io API communication.
 * 
 * <p>Handles all HTTP requests to Fixer.io and converts responses
 * to internal DTOs.</p>
 */
@Slf4j
@Component
public class FixerClient {
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    
    public FixerClient(
            RestTemplate restTemplate,
            @Value("${exchange.provider.fixer.base-url}") String baseUrl,
            @Value("${exchange.provider.fixer.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }
    
    /**
     * Fetches exchange rate from Fixer.io API.
     * 
     * @param from base currency code
     * @param to target currency code
     * @return FixerResponse containing rate information
     * @throws ExternalProviderException if API call fails
     */
    public FixerResponse getExchangeRate(String from, String to) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/latest")
                .queryParam("access_key", apiKey)
                .queryParam("base", from)
                .queryParam("symbols", to)
                .build()
                .toUriString();
        
        log.debug("Calling Fixer.io API: {} -> {}", from, to);
        
        try {
            FixerResponse response = restTemplate.getForObject(url, FixerResponse.class);
            
            if (response == null) {
                throw new ExternalProviderException("Fixer.io returned null response");
            }
            
            if (!response.isSuccess()) {
                String errorMsg = response.getError() != null 
                        ? response.getError().getInfo() 
                        : "Unknown error";
                throw new ExternalProviderException("Fixer.io API error: " + errorMsg);
            }
            
            log.debug("Successfully fetched rate from Fixer.io: {} -> {} = {}", 
                    from, to, response.getRates().get(to));
            
            return response;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                throw new ExternalProviderException("Fixer.io authentication failed. Check API key.", e);
            } else if (e.getStatusCode().value() == 429) {
                throw new ExternalProviderException("Fixer.io rate limit exceeded.", e);
            }
            throw new ExternalProviderException("Fixer.io client error: " + e.getMessage(), e);
            
        } catch (HttpServerErrorException e) {
            throw new ExternalProviderException("Fixer.io server error: " + e.getMessage(), e);
            
        } catch (ResourceAccessException e) {
            throw new ExternalProviderException("Network error calling Fixer.io: " + e.getMessage(), e);
            
        } catch (Exception e) {
            if (e instanceof ExternalProviderException) {
                throw e;
            }
            throw new ExternalProviderException("Unexpected error calling Fixer.io: " + e.getMessage(), e);
        }
    }
}
