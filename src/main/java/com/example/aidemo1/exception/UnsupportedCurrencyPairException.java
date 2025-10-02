package com.example.aidemo1.exception;

/**
 * Exception thrown when a provider does not support a requested currency pair.
 * 
 * <p>This exception indicates that while the provider is operational,
 * it cannot handle the specific currency pair requested. This is different
 * from {@link ExternalProviderException} which indicates a provider failure.</p>
 * 
 * <p>Common scenarios:</p>
 * <ul>
 *   <li>Provider only supports a limited set of currencies</li>
 *   <li>Currency code is invalid or not recognized by the provider</li>
 *   <li>Currency pair is not available in the provider's API</li>
 *   <li>Base currency is not supported by the provider</li>
 * </ul>
 * 
 * <p>The rate aggregator should catch this exception and try alternative
 * providers that support the requested currency pair. Implementations
 * should preferably use the {@link com.example.aidemo1.integration.provider.ExchangeRateProvider#supports(String, String)}
 * method to check support before calling {@link com.example.aidemo1.integration.provider.ExchangeRateProvider#fetchRate(String, String)}.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * if (provider.supports("USD", "EUR")) {
 *     ExchangeRate rate = provider.fetchRate("USD", "EUR");
 * } else {
 *     throw new UnsupportedCurrencyPairException(
 *         "USD/EUR", provider.getProviderName()
 *     );
 * }
 * }</pre>
 */
public class UnsupportedCurrencyPairException extends RuntimeException {
    
    private final String currencyPair;
    private final String providerName;
    
    /**
     * Constructs a new exception with the specified currency pair and provider.
     * 
     * @param currencyPair the unsupported currency pair (e.g., "USD/EUR")
     * @param providerName the name of the provider that doesn't support this pair
     */
    public UnsupportedCurrencyPairException(String currencyPair, String providerName) {
        super(String.format("Currency pair '%s' is not supported by provider '%s'", 
                          currencyPair, providerName));
        this.currencyPair = currencyPair;
        this.providerName = providerName;
    }
    
    /**
     * Constructs a new exception with base and target currencies and provider name.
     * 
     * @param fromCurrency the base currency code (e.g., "USD")
     * @param toCurrency the target currency code (e.g., "EUR")
     * @param providerName the name of the provider that doesn't support this pair
     */
    public UnsupportedCurrencyPairException(String fromCurrency, String toCurrency, String providerName) {
        this(fromCurrency + "/" + toCurrency, providerName);
    }
    
    /**
     * Returns the unsupported currency pair.
     * 
     * @return the currency pair (e.g., "USD/EUR")
     */
    public String getCurrencyPair() {
        return currencyPair;
    }
    
    /**
     * Returns the provider name that doesn't support the currency pair.
     * 
     * @return the provider name
     */
    public String getProviderName() {
        return providerName;
    }
}
