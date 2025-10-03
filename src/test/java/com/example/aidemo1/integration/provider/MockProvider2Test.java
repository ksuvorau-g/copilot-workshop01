package com.example.aidemo1.integration.provider;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.exception.UnsupportedCurrencyPairException;
import com.example.aidemo1.integration.client.MockProvider2Client;
import com.example.aidemo1.integration.dto.external.MockProviderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockProvider2Test {

    @Mock
    private MockProvider2Client client;

    @InjectMocks
    private MockProvider2 provider;

    private MockProviderResponse successResponse;

    @BeforeEach
    void setUp() {
        successResponse = MockProviderResponse.builder()
                .success(true)
                .base("USD")
                .target("EUR")
                .rate(new BigDecimal("0.87"))
                .timestamp(Instant.now().getEpochSecond())
                .provider("Mock Provider 2")
                .build();
    }

    @Test
    void testFetchRate_Success() {
        when(client.getExchangeRate("USD", "EUR")).thenReturn(successResponse);

        ExchangeRate result = provider.fetchRate("USD", "EUR");

        assertNotNull(result);
        assertEquals("USD", result.getBaseCurrency());
        assertEquals("EUR", result.getTargetCurrency());
        assertEquals(new BigDecimal("0.87"), result.getRate());
        assertEquals("Mock Provider 2", result.getProvider());
        assertNotNull(result.getTimestamp());
        
        verify(client, times(1)).getExchangeRate("USD", "EUR");
    }

    @Test
    void testFetchRate_NullBaseCurrency() {
        assertThrows(IllegalArgumentException.class, () -> provider.fetchRate(null, "EUR"));
        verify(client, never()).getExchangeRate(any(), any());
    }

    @Test
    void testFetchRate_EmptyBaseCurrency() {
        assertThrows(IllegalArgumentException.class, () -> provider.fetchRate("", "EUR"));
        verify(client, never()).getExchangeRate(any(), any());
    }

    @Test
    void testFetchRate_NullTargetCurrency() {
        assertThrows(IllegalArgumentException.class, () -> provider.fetchRate("USD", null));
        verify(client, never()).getExchangeRate(any(), any());
    }

    @Test
    void testFetchRate_EmptyTargetCurrency() {
        assertThrows(IllegalArgumentException.class, () -> provider.fetchRate("USD", ""));
        verify(client, never()).getExchangeRate(any(), any());
    }

    @Test
    void testFetchRate_NullRate() {
        successResponse.setRate(null);
        when(client.getExchangeRate("USD", "EUR")).thenReturn(successResponse);

        assertThrows(UnsupportedCurrencyPairException.class, () -> provider.fetchRate("USD", "EUR"));
        verify(client, times(1)).getExchangeRate("USD", "EUR");
    }

    @Test
    void testFetchRate_ClientException() {
        when(client.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalProviderException("Network error"));

        assertThrows(ExternalProviderException.class, () -> provider.fetchRate("USD", "EUR"));
        verify(client, times(1)).getExchangeRate("USD", "EUR");
    }

    @Test
    void testSupports_ValidCurrencies() {
        assertTrue(provider.supports("USD", "EUR"));
        assertTrue(provider.supports("GBP", "JPY"));
    }

    @Test
    void testSupports_NullBaseCurrency() {
        assertFalse(provider.supports(null, "EUR"));
    }

    @Test
    void testSupports_EmptyBaseCurrency() {
        assertFalse(provider.supports("", "EUR"));
    }

    @Test
    void testSupports_NullTargetCurrency() {
        assertFalse(provider.supports("USD", null));
    }

    @Test
    void testSupports_EmptyTargetCurrency() {
        assertFalse(provider.supports("USD", ""));
    }

    @Test
    void testGetProviderName() {
        assertEquals("Mock Provider 2", provider.getProviderName());
    }

    @Test
    void testGetPriority() {
        assertEquals(50, provider.getPriority());
    }

    @Test
    void testFetchRate_CaseConversion() {
        when(client.getExchangeRate("usd", "eur")).thenReturn(successResponse);

        ExchangeRate result = provider.fetchRate("usd", "eur");

        assertNotNull(result);
        assertEquals("USD", result.getBaseCurrency());
        assertEquals("EUR", result.getTargetCurrency());
    }
}
