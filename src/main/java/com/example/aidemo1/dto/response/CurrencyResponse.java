package com.example.aidemo1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for currency information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyResponse {

    /**
     * Currency code (ISO 4217 format).
     * Example: USD, EUR, GBP
     */
    private String code;

    /**
     * Human-readable name of the currency.
     * Example: "US Dollar", "Euro", "British Pound"
     */
    private String name;

    /**
     * Timestamp when the currency was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the currency was last updated.
     */
    private LocalDateTime updatedAt;
}
