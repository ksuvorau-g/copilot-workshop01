package com.example.aidemo1.integration.client;

import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.dto.external.FixerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixerClientTest {

    @Mock
    private RestTemplate restTemplate;

    private FixerClient client;

    private static final String BASE_URL = "http://data.fixer.io/api";
    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        client = new FixerClient(restTemplate, BASE_URL, API_KEY);
    }

    @Test
    void testGetExchangeRate_Success() {
        FixerResponse mockResponse = new FixerResponse();
        mockResponse.setSuccess(true);
        mockResponse.setTimestamp(System.currentTimeMillis() / 1000);
        mockResponse.setBase("USD");
        
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenReturn(mockResponse);

        FixerResponse result = client.getExchangeRate("USD", "EUR");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("0.85"), result.getRates().get("EUR"));
    }

    @Test
    void testGetExchangeRate_NullResponse() {
        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenReturn(null);

        assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
    }

    @Test
    void testGetExchangeRate_ApiError() {
        FixerResponse mockResponse = new FixerResponse();
        mockResponse.setSuccess(false);
        
        FixerResponse.ErrorInfo errorInfo = new FixerResponse.ErrorInfo();
        errorInfo.setInfo("Invalid API key");
        mockResponse.setError(errorInfo);

        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenReturn(mockResponse);

        ExternalProviderException exception = assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(exception.getMessage().contains("Invalid API key"));
    }

    @Test
    void testGetExchangeRate_AuthenticationError() {
        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        ExternalProviderException exception = assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(exception.getMessage().contains("authentication failed"));
    }

    @Test
    void testGetExchangeRate_RateLimitError() {
        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        ExternalProviderException exception = assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(exception.getMessage().contains("rate limit"));
    }

    @Test
    void testGetExchangeRate_ServerError() {
        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        ExternalProviderException exception = assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(exception.getMessage().contains("server error"));
    }

    @Test
    void testGetExchangeRate_NetworkError() {
        when(restTemplate.getForObject(any(String.class), eq(FixerResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        ExternalProviderException exception = assertThrows(ExternalProviderException.class, 
                () -> client.getExchangeRate("USD", "EUR"));
        assertTrue(exception.getMessage().contains("Network error"));
    }
}
