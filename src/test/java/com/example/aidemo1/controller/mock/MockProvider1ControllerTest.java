package com.example.aidemo1.controller.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for MockProvider1Controller.
 */
@WebMvcTest(MockProvider1Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class MockProvider1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetExchangeRate_Success() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "USD")
                        .param("target", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.target").value("EUR"))
                .andExpect(jsonPath("$.rate").isNumber())
                .andExpect(jsonPath("$.rate").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.rate").value(lessThan(3.0)))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.provider").value("Mock Provider 1"));
    }

    @Test
    void testGetExchangeRate_MissingBaseParameter() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("target", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetExchangeRate_MissingTargetParameter() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "USD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetExchangeRate_EmptyBaseParameter() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "")
                        .param("target", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Base and target currency codes are required"));
    }

    @Test
    void testGetExchangeRate_EmptyTargetParameter() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "USD")
                        .param("target", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Base and target currency codes are required"));
    }

    @Test
    void testGetExchangeRate_DifferentCurrencyPairs() throws Exception {
        // Test multiple currency pairs to ensure randomness
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "GBP")
                        .param("target", "JPY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("GBP"))
                .andExpect(jsonPath("$.target").value("JPY"))
                .andExpect(jsonPath("$.rate").isNumber());

        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "CAD")
                        .param("target", "AUD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("CAD"))
                .andExpect(jsonPath("$.target").value("AUD"))
                .andExpect(jsonPath("$.rate").isNumber());
    }
}
