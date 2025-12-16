package com.example.aidemo1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response object containing exchange rate calculation results")
public class ExchangeRateResponse {

    /**
     * Source currency code.
     * Example: USD
     */
    @Schema(
            description = "Source currency code",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String from;

    /**
     * Target currency code.
     * Example: EUR
     */
    @Schema(
            description = "Target currency code",
            example = "EUR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String to;

    /**
     * Original amount in source currency.
     * Example: 100.00
     */
    @Schema(
            description = "Original amount in source currency",
            example = "100.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    /**
     * Exchange rate applied.
     * Example: 0.85 (1 USD = 0.85 EUR)
     */
    @Schema(
            description = "Exchange rate from source to target currency (1 source = X target)",
            example = "0.85",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal rate;

    /**
     * Converted amount in target currency.
     * Example: 85.00
     */
    @Schema(
            description = "Converted amount in target currency",
            example = "85.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal convertedAmount;

    /**
     * Provider that supplied this rate.
     * Example: "Fixer", "ExchangeRatesAPI", "MockProvider1"
     */
    @Schema(
            description = "Name of the provider that supplied this exchange rate",
            example = "Fixer",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String provider;

    /**
     * Timestamp when the rate was retrieved or calculated.
     */
    @Schema(
            description = "Timestamp when the rate was retrieved from the provider",
            example = "2025-10-15T14:30:00"
    )
    private LocalDateTime timestamp;
}
