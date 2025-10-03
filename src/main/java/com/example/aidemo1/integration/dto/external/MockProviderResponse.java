package com.example.aidemo1.integration.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MockProviderResponse {
    
    /**
     * Indicates whether the request was successful.
     */
    @JsonProperty("success")
    private Boolean success;
    
    /**
     * The base currency code (e.g., "USD").
     */
    @JsonProperty("base")
    private String base;
    
    /**
     * The target currency code (e.g., "EUR").
     */
    @JsonProperty("target")
    private String target;
    
    /**
     * The exchange rate from base to target currency.
     */
    @JsonProperty("rate")
    private BigDecimal rate;
    
    /**
     * Unix timestamp of when the rate was generated.
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * Name of the mock provider (e.g., "Mock Provider 1").
     */
    @JsonProperty("provider")
    private String provider;
    
    /**
     * Error message if success is false.
     */
    @JsonProperty("error")
    private String error;
}
