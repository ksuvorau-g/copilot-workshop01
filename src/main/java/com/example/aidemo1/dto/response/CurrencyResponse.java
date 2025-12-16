package com.example.aidemo1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response object containing currency information")
public class CurrencyResponse {

    /**
     * Currency code (ISO 4217 format).
     * Example: USD, EUR, GBP
     */
    @Schema(
            description = "Currency code in ISO 4217 format (3 uppercase letters)",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * Human-readable name of the currency.
     * Example: "US Dollar", "Euro", "British Pound"
     */
    @Schema(
            description = "Human-readable name of the currency",
            example = "US Dollar"
    )
    private String name;

    /**
     * Timestamp when the currency was created.
     */
    @Schema(
            description = "Timestamp when the currency was created in the system",
            example = "2025-10-15T10:30:00"
    )
    private LocalDateTime createdAt;

    /**
     * Timestamp when the currency was last updated.
     */
    @Schema(
            description = "Timestamp when the currency was last updated",
            example = "2025-10-15T14:45:00"
    )
    private LocalDateTime updatedAt;
}
