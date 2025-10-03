package com.example.aidemo1.integration.provider;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.exception.UnsupportedCurrencyPairException;
import com.example.aidemo1.integration.client.FixerClient;
import com.example.aidemo1.integration.dto.external.FixerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixerProviderTest {

    @Mock
    private FixerClient client;

    @InjectMocks
    private FixerProvider provider;

    private FixerResponse successResponse;

    @BeforeEach
    void setUp() {
        successResponse = new FixerResponse();
        successResponse.setSuccess(true);
        successResponse.setTimestamp(Instant.now().getEpochSecond());
        successResponse.setBase("USD");
        successResponse.setDate("2021-03-17");
        
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        successResponse.setRates(rates);
    }

    @Test
    void testFetchRate_Success() {
        when(client.getExchangeRate("USD", "EUR")).thenReturn(successResponse);

        ExchangeRate result = provider.fetchRate("USD", "EUR");

        assertNotNull(result);
        assertEquals("USD", result.getBaseCurrency());
        assertEquals("EUR", result.getTargetCurrency());
        assertEquals(new BigDecimal("0.85"), result.getRate());
        assertEquals("Fixer", result.getProvider());
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
    void testFetchRate_UnsupportedCurrencyPair_NoRates() {
        successResponse.setRates(new HashMap<>());
        when(client.getExchangeRate("USD", "XYZ")).thenReturn(successResponse);

        assertThrows(UnsupportedCurrencyPairException.class, 
                () -> provider.fetchRate("USD", "XYZ"));
    }

    @Test
    void testFetchRate_UnsupportedCurrencyPair_MissingRate() {
        when(client.getExchangeRate("USD", "XYZ")).thenReturn(successResponse);

        assertThrows(UnsupportedCurrencyPairException.class, 
                () -> provider.fetchRate("USD", "XYZ"));
    }

    @Test
    void testFetchRate_ClientException() {
        when(client.getExchangeRate("USD", "EUR"))
                .thenThrow(new ExternalProviderException("Network error"));

        assertThrows(ExternalProviderException.class, 
                () -> provider.fetchRate("USD", "EUR"));
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
        assertEquals("Fixer", provider.getProviderName());
    }

    @Test
    void testGetPriority() {
        assertEquals(100, provider.getPriority());
    }
}
