package com.example.aidemo1.exception;

/**
 * Exception thrown when an external exchange rate provider fails to fetch rates.
 * 
 * <p>This exception indicates that a provider encountered an error while attempting
 * to retrieve exchange rate data from an external source. Common causes include:</p>
 * <ul>
 *   <li>Network connectivity issues</li>
 *   <li>API authentication failures</li>
 *   <li>API rate limit exceeded</li>
 *   <li>Provider service downtime</li>
 *   <li>Invalid API response format</li>
 *   <li>Timeout during API call</li>
 * </ul>
 * 
 * <p>This exception should be caught by the rate aggregator to implement
 * fallback mechanisms and try alternative providers.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     ExchangeRate rate = provider.fetchRate("USD", "EUR");
 * } catch (ExternalProviderException e) {
 *     logger.error("Provider {} failed: {}", provider.getProviderName(), e.getMessage());
 *     // Try next provider
 * }
 * }</pre>
 */
public class ExternalProviderException extends RuntimeException {
    
    /**
     * Constructs a new exception with the specified detail message.
     * 
     * @param message the detail message explaining why the provider failed
     */
    public ExternalProviderException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * <p>This constructor is useful when wrapping lower-level exceptions
     * such as network errors or JSON parsing failures.</p>
     * 
     * @param message the detail message explaining why the provider failed
     * @param cause the underlying cause of the failure
     */
    public ExternalProviderException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     * 
     * @param cause the underlying cause of the failure
     */
    public ExternalProviderException(Throwable cause) {
        super(cause);
    }
}
