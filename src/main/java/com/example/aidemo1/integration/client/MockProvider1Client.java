package com.example.aidemo1.integration.client;

import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.dto.external.MockProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST client for Mock Provider 1 internal endpoint.
 * 
 * <p>This client communicates with the internal mock provider endpoint
 * to fetch simulated exchange rates. It is primarily used for testing
 * and development when external providers are not available or configured.</p>
 * 
 * <h2>Configuration</h2>
 * <p>Base URL is configured via {@code mock.provider1.base-url} property
 * with a default of {@code http://localhost:8080}.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MockProvider1Client client = ...;
 * MockProviderResponse response = client.getExchangeRate("USD", "EUR");
 * System.out.println("Rate: " + response.getRate());
 * }</pre>
 */
@Slf4j
@Component
public class MockProvider1Client {
    
    private static final String PROVIDER_NAME = "Mock Provider 1";
    private static final String RATE_ENDPOINT = "/mock/provider1/rate";
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    /**
     * Constructs a new MockProvider1Client with the specified base URL.
     * 
     * @param restTemplate the REST template for HTTP calls
     * @param baseUrl the base URL of the mock provider endpoint
     */
    public MockProvider1Client(
            RestTemplate restTemplate,
            @Value("${mock.provider1.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Fetches exchange rate from Mock Provider 1 internal endpoint.
     * 
     * <p>Calls the internal {@code /mock/provider1/rate} endpoint with the
     * specified currency pair and returns the generated mock rate.</p>
     * 
     * @param from the base currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return the mock provider response containing the rate
     * @throws ExternalProviderException if the request fails or returns invalid data
     * @throws IllegalArgumentException if currency codes are null or empty
     */
    public MockProviderResponse getExchangeRate(String from, String to) {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Base currency cannot be null or empty");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Target currency cannot be null or empty");
        }
        
        String url = UriComponentsBuilder.fromUriString(baseUrl + RATE_ENDPOINT)
                .queryParam("base", from)
                .queryParam("target", to)
                .build()
                .toUriString();
        
        log.debug("Calling {}: {}", PROVIDER_NAME, url);
        
        try {
            MockProviderResponse response = restTemplate.getForObject(url, MockProviderResponse.class);
            
            if (response == null) {
                throw new ExternalProviderException(
                        "Received null response from " + PROVIDER_NAME
                );
            }
            
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new ExternalProviderException(
                        "Provider returned error: " + response.getError()
                );
            }
            
            log.debug("{} returned rate: {} -> {} = {}", 
                    PROVIDER_NAME, from, to, response.getRate());
            
            return response;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException(
                        "Invalid request to " + PROVIDER_NAME + ": " + e.getMessage(),
                        e
                );
            }
            throw new ExternalProviderException(
                    "Client error from " + PROVIDER_NAME + ": " + e.getMessage(),
                    e
            );
            
        } catch (HttpServerErrorException e) {
            throw new ExternalProviderException(
                    "Server error from " + PROVIDER_NAME + ": " + e.getMessage(),
                    e
            );
            
        } catch (RestClientException e) {
            throw new ExternalProviderException(
                    "Failed to connect to " + PROVIDER_NAME + ": " + e.getMessage(),
                    e
            );
        }
    }
}
