package com.example.aidemo1.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for mapping responses from ExchangeRatesAPI.io.
 * 
 * Example response:
 * {
 *   "success": true,
 *   "timestamp": 1609459199,
 *   "base": "USD",
 *   "date": "2021-01-01",
 *   "rates": {
 *     "EUR": 0.817623,
 *     "GBP": 0.735234
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRatesApiResponse {

    /**
     * Indicates if the API request was successful.
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Unix timestamp of the rate data.
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Base currency code.
     */
    @JsonProperty("base")
    private String base;

    /**
     * Date of the rate data.
     */
    @JsonProperty("date")
    private LocalDate date;

    /**
     * Map of currency codes to exchange rates.
     * Key: target currency code (e.g., "EUR")
     * Value: exchange rate (e.g., 0.817623)
     */
    @JsonProperty("rates")
    private Map<String, BigDecimal> rates;

    /**
     * Error information (present only when success is false).
     */
    @JsonProperty("error")
    private ExchangeRatesApiError error;

    /**
     * Inner class for error details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExchangeRatesApiError {
        
        @JsonProperty("code")
        private Integer code;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("info")
        private String info;
    }
}
