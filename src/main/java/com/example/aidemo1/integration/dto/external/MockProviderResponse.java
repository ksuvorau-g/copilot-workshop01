package com.example.aidemo1.integration.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for mock exchange rate provider endpoints.
 * 
 * <p>This DTO represents the JSON response structure returned by internal
 * mock provider endpoints. Mock providers simulate external API behavior
 * for testing and development purposes.</p>
 * 
 * <h2>JSON Structure Example</h2>
 * <pre>{@code
 * {
 *   "success": true,
 *   "base": "USD",
 *   "target": "EUR",
 *   "rate": 0.85,
 *   "timestamp": 1696320000,
 *   "provider": "Mock Provider 1"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object from mock exchange rate provider endpoints (for testing)")
public class MockProviderResponse {
    
    /**
     * Indicates whether the request was successful.
     */
    @Schema(
            description = "Indicates whether the request was successful",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("success")
    private Boolean success;
    
    /**
     * The base currency code (e.g., "USD").
     */
    @Schema(
            description = "Base currency code (ISO 4217 format)",
            example = "USD"
    )
    @JsonProperty("base")
    private String base;
    
    /**
     * The target currency code (e.g., "EUR").
     */
    @Schema(
            description = "Target currency code (ISO 4217 format)",
            example = "EUR"
    )
    @JsonProperty("target")
    private String target;
    
    /**
     * The exchange rate from base to target currency.
     */
    @Schema(
            description = "Exchange rate from base to target currency",
            example = "0.850000"
    )
    @JsonProperty("rate")
    private BigDecimal rate;
    
    /**
     * Unix timestamp of when the rate was generated.
     */
    @Schema(
            description = "Unix timestamp (seconds since epoch) when the rate was generated",
            example = "1696320000"
    )
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * Name of the mock provider (e.g., "Mock Provider 1").
     */
    @Schema(
            description = "Name of the mock provider",
            example = "Mock Provider 1"
    )
    @JsonProperty("provider")
    private String provider;
    
    /**
     * Error message if success is false.
     */
    @Schema(
            description = "Error message (only present when success is false)",
            example = "Base and target currency codes are required"
    )
    @JsonProperty("error")
    private String error;
}
