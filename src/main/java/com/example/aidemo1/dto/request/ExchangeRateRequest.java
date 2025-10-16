package com.example.aidemo1.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for getting an exchange rate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateRequest {

    /**
     * Source currency code (ISO 4217 format).
     * Example: USD, EUR, GBP
     */
    @NotBlank(message = "Source currency (from) is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    private String from;

    /**
     * Target currency code (ISO 4217 format).
     * Example: USD, EUR, GBP
     */
    @NotBlank(message = "Target currency (to) is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    private String to;

    /**
     * Amount to convert (must be positive).
     * Example: 100.00
     */
    @Positive(message = "Amount must be greater than zero")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;
}
