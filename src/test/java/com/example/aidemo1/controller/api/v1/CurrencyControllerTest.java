package com.example.aidemo1.controller.api.v1;

import com.example.aidemo1.dto.request.AddCurrencyRequest;
import com.example.aidemo1.dto.response.CurrencyResponse;
import com.example.aidemo1.dto.response.ExchangeRateResponse;
import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.CurrencyNotFoundException;
import com.example.aidemo1.exception.ExchangeRateNotFoundException;
import com.example.aidemo1.service.CurrencyService;
import com.example.aidemo1.service.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for CurrencyController.
 * Uses @WebMvcTest to test the controller layer in isolation.
 */
@WebMvcTest(CurrencyController.class)
@AutoConfigureMockMvc(addFilters = false)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET /api/v1/currencies Tests ====================

    @Test
    void getCurrencies_WhenCurrenciesExist_ReturnsListOfCurrencies() throws Exception {
        // Given
        Currency usd = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Currency eur = Currency.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(currencyService.getAllCurrencies()).thenReturn(List.of(usd, eur));

        // When & Then
        mockMvc.perform(get("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("USD"))
                .andExpect(jsonPath("$[0].name").value("US Dollar"))
                .andExpect(jsonPath("$[1].code").value("EUR"))
                .andExpect(jsonPath("$[1].name").value("Euro"));
    }

    @Test
    void getCurrencies_WhenNoCurrenciesExist_ReturnsEmptyList() throws Exception {
        // Given
        when(currencyService.getAllCurrencies()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== GET /api/v1/currencies/exchange-rates Tests ====================

    @Test
    void getExchangeRate_WhenCurrenciesExist_ReturnsRate() throws Exception {
        // Given
        ExchangeRate rateEntity = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.850000"))
                .provider("Mock Provider 1")
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateService.getExchangeRateEntity("USD", "EUR"))
                .thenReturn(rateEntity);
        when(exchangeRateService.getExchangeRate("USD", "EUR", BigDecimal.ONE))
                .thenReturn(new BigDecimal("0.850000"));

        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.rate").value(0.850000))
                .andExpect(jsonPath("$.convertedAmount").value(0.850000))
                .andExpect(jsonPath("$.provider").value("Mock Provider 1"));
    }

    @Test
    void getExchangeRate_WithCustomAmount_CalculatesConvertedAmount() throws Exception {
        // Given
        ExchangeRate rateEntity = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.850000"))
                .provider("Mock Provider 1")
                .timestamp(LocalDateTime.now())
                .build();

        BigDecimal amount = new BigDecimal("100");
        BigDecimal convertedAmount = new BigDecimal("85.000000");

        when(exchangeRateService.getExchangeRateEntity("USD", "EUR"))
                .thenReturn(rateEntity);
        when(exchangeRateService.getExchangeRate("USD", "EUR", amount))
                .thenReturn(convertedAmount);

        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.rate").value(0.850000))
                .andExpect(jsonPath("$.convertedAmount").value(85.000000));
    }

    @Test
    void getExchangeRate_WhenCurrencyNotFound_ReturnsNotFound() throws Exception {
        // Given
        when(exchangeRateService.getExchangeRateEntity("USD", "XXX"))
                .thenThrow(CurrencyNotFoundException.forCode("XXX"));

        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "XXX")
                        .param("amount", "100"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExchangeRate_WhenMissingFromParameter_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("to", "EUR")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_WhenMissingToParameter_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("amount", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_WhenMissingAmountParameter_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_WhenAmountIsZero_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_WhenAmountIsNegative_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("amount", "-100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_NormalizesCurrencyCodes_ToUppercase() throws Exception {
        // Given
        ExchangeRate rateEntity = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.850000"))
                .provider("Mock Provider 1")
                .timestamp(LocalDateTime.now())
                .build();

        when(exchangeRateService.getExchangeRateEntity("USD", "EUR"))
                .thenReturn(rateEntity);
        when(exchangeRateService.getExchangeRate("USD", "EUR", BigDecimal.ONE))
                .thenReturn(new BigDecimal("0.850000"));

        // When & Then - lowercase input should be normalized to uppercase
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("from", "usd")
                        .param("to", "eur")
                        .param("amount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"));
    }

    // ==================== POST /api/v1/currencies Tests ====================

    @Test
    void addCurrency_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .code("USD")
                .name("US Dollar")
                .build();

        Currency currency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(currencyService.addCurrency("USD")).thenReturn(currency);

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.name").value("US Dollar"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void addCurrency_WhenDuplicate_ReturnsConflict() throws Exception {
        // Given
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .code("USD")
                .name("US Dollar")
                .build();

        when(currencyService.addCurrency("USD"))
                .thenThrow(new IllegalArgumentException("Currency already exists: USD"));

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCurrency_WithInvalidCode_ReturnsBadRequest() throws Exception {
        // Given - code with only 2 characters
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .code("US")
                .name("US Dollar")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCurrency_WithInvalidCodePattern_ReturnsBadRequest() throws Exception {
        // Given - code with lowercase letters
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .code("usd")
                .name("US Dollar")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCurrency_WithMissingCode_ReturnsBadRequest() throws Exception {
        // Given
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .name("US Dollar")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCurrency_WithEmptyCode_ReturnsBadRequest() throws Exception {
        // Given
        AddCurrencyRequest request = AddCurrencyRequest.builder()
                .code("")
                .name("US Dollar")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== POST /api/v1/currencies/refresh Tests ====================

    @Test
    void refreshRates_WhenSuccessful_ReturnsOkWithCount() throws Exception {
        // Given
        when(exchangeRateService.refreshAllRates()).thenReturn(12);

        // When & Then
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully refreshed exchange rates"))
                .andExpect(jsonPath("$.refreshedPairs").value(12))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refreshRates_WhenNoRatesRefreshed_ReturnsOkWithZeroCount() throws Exception {
        // Given
        when(exchangeRateService.refreshAllRates()).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully refreshed exchange rates"))
                .andExpect(jsonPath("$.refreshedPairs").value(0))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
