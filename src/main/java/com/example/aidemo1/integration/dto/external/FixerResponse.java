package com.example.aidemo1.integration.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Fixer.io API response.
 * 
 * <p>Represents the JSON response from Fixer.io exchange rate API.
 * Example response:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "timestamp": 1519296206,
 *   "base": "USD",
 *   "date": "2021-03-17",
 *   "rates": {
 *     "EUR": 0.813399
 *   }
 * }
 * }</pre>
 * 
 * @see <a href="https://fixer.io/documentation">Fixer.io Documentation</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FixerResponse {
    
    /**
     * Indicates if the API call was successful.
     */
    private boolean success;
    
    /**
     * Unix timestamp of the rate.
     */
    private long timestamp;
    
    /**
     * Base currency code.
     */
    private String base;
    
    /**
     * Date of the exchange rate (YYYY-MM-DD format).
     */
    private String date;
    
    /**
     * Map of target currencies to their exchange rates.
     * Key: currency code (e.g., "EUR")
     * Value: exchange rate as BigDecimal
     */
    private Map<String, BigDecimal> rates;
    
    /**
     * Error information if success is false.
     */
    @JsonProperty("error")
    private ErrorInfo error;
    
    /**
     * Nested error information.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorInfo {
        private int code;
        private String type;
        private String info;
    }
}
