package com.example.aidemo1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for exchange rate calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponse {

    /**
     * Source currency code.
     * Example: USD
     */
    private String from;

    /**
     * Target currency code.
     * Example: EUR
     */
    private String to;

    /**
     * Original amount in source currency.
     * Example: 100.00
     */
    private BigDecimal amount;

    /**
     * Exchange rate applied.
     * Example: 0.85 (1 USD = 0.85 EUR)
     */
    private BigDecimal rate;

    /**
     * Converted amount in target currency.
     * Example: 85.00
     */
    private BigDecimal convertedAmount;

    /**
     * Provider that supplied this rate.
     * Example: "Fixer", "ExchangeRatesAPI", "MockProvider1"
     */
    private String provider;

    /**
     * Timestamp when the rate was retrieved or calculated.
     */
    private LocalDateTime timestamp;
}
