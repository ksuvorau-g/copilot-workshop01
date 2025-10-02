package com.example.aidemo1.integration.provider;

import com.example.aidemo1.entity.ExchangeRate;

/**
 * Core interface for all exchange rate providers (real and mock).
 * Establishes the contract for fetching exchange rates from external sources.
 * 
 * <p>Implementations should handle their own error management and convert
 * provider-specific responses into the common {@link ExchangeRate} entity.</p>
 * 
 * <h2>Priority-Based Selection</h2>
 * <p>Providers are selected based on their priority value returned by {@link #getPriority()}.
 * Higher priority values indicate preferred providers:</p>
 * <ul>
 *   <li>Real providers (Fixer, ExchangeRatesAPI): priority = 100</li>
 *   <li>Mock providers: priority = 50</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExchangeRateProvider provider = ...;
 * 
 * // Check if provider supports the currency pair
 * if (provider.supports("USD", "EUR")) {
 *     // Fetch the exchange rate
 *     ExchangeRate rate = provider.fetchRate("USD", "EUR");
 *     System.out.println("Rate from " + provider.getProviderName() + 
 *                        " (priority: " + provider.getPriority() + "): " + rate.getRate());
 * }
 * }</pre>
 * 
 * @see ExchangeRate
 */
public interface ExchangeRateProvider {
    
    /**
     * Fetches the current exchange rate for the specified currency pair.
     * 
     * <p>This method should return the most current exchange rate available
     * from the provider. The returned {@link ExchangeRate} entity should be
     * fully populated with all required fields including timestamp and provider name.</p>
     * 
     * @param from the base currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return the exchange rate information as an {@link ExchangeRate} entity
     * @throws com.example.aidemo1.exception.ExternalProviderException if the provider fails to fetch the rate
     * @throws com.example.aidemo1.exception.UnsupportedCurrencyPairException if the currency pair is not supported
     * @throws IllegalArgumentException if currency codes are null or empty
     */
    ExchangeRate fetchRate(String from, String to);
    
    /**
     * Checks whether this provider supports the specified currency pair.
     * 
     * <p>This method should perform a quick check (without making external API calls)
     * to determine if the provider can handle the given currency pair. It allows
     * the rate aggregator to filter providers before attempting to fetch rates.</p>
     * 
     * @param from the base currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return {@code true} if the provider supports this currency pair, {@code false} otherwise
     * @throws IllegalArgumentException if currency codes are null or empty
     */
    boolean supports(String from, String to);
    
    /**
     * Returns a unique identifier for this provider.
     * 
     * <p>The provider name should be unique across all implementations and
     * will be stored in the {@link ExchangeRate} entity to track the source
     * of each rate.</p>
     * 
     * <p>Recommended naming conventions:</p>
     * <ul>
     *   <li>Real providers: "Fixer", "ExchangeRatesAPI"</li>
     *   <li>Mock providers: "MockProvider1", "MockProvider2"</li>
     * </ul>
     * 
     * @return a unique string identifier for this provider, never null or empty
     */
    String getProviderName();
    
    /**
     * Returns the priority of this provider for rate selection.
     * 
     * <p>Higher priority values indicate that this provider should be preferred
     * over others when multiple providers support the same currency pair.
     * The rate aggregator uses this value to determine the order in which
     * providers are queried.</p>
     * 
     * <p>Standard priority values:</p>
     * <ul>
     *   <li><b>100</b>: Real external providers (Fixer, ExchangeRatesAPI)</li>
     *   <li><b>50</b>: Mock/test providers</li>
     * </ul>
     * 
     * @return the priority value, where higher values indicate higher priority
     */
    int getPriority();
}
