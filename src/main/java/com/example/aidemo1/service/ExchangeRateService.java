package com.example.aidemo1.service;

import com.example.aidemo1.entity.ExchangeRate;

import java.math.BigDecimal;

/**
 * Service interface for exchange rate operations.
 * Provides business logic for fetching, caching, and managing exchange rates.
 * 
 * <p>This service implements a three-tier lookup strategy:</p>
 * <ol>
 *   <li><strong>Cache:</strong> Check Redis cache for recent rates</li>
 *   <li><strong>Database:</strong> Query database for historical rates</li>
 *   <li><strong>Providers:</strong> Fetch fresh rates from external providers</li>
 * </ol>
 */
public interface ExchangeRateService {

    /**
     * Gets the exchange rate for a currency pair and calculates the converted amount.
     * 
     * <p>Lookup strategy:</p>
     * <ol>
     *   <li>Check Redis cache</li>
     *   <li>If not cached, check database for recent rate (within last hour)</li>
     *   <li>If no recent rate, fetch from providers via RateAggregator</li>
     *   <li>Cache and save the fetched rate</li>
     * </ol>
     *
     * @param from   the base currency code (e.g., "USD")
     * @param to     the target currency code (e.g., "EUR")
     * @param amount the amount to convert
     * @return the converted amount in the target currency
     * @throws com.example.aidemo1.exception.CurrencyNotFoundException if currency doesn't exist
     * @throws com.example.aidemo1.exception.ExchangeRateNotFoundException if rate cannot be obtained
     * @throws IllegalArgumentException if inputs are invalid
     */
    BigDecimal getExchangeRate(String from, String to, BigDecimal amount);

    /**
     * Refreshes exchange rates for all supported currency pairs.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Fetches fresh rates from all providers</li>
     *   <li>Saves all rates to the database</li>
     *   <li>Updates the Redis cache with best rates</li>
     *   <li>Returns the count of successfully refreshed pairs</li>
     * </ul>
     * 
     * <p>Typically called by a scheduler on a regular interval.</p>
     *
     * @return the number of currency pairs successfully refreshed
     */
    int refreshAllRates();

    /**
     * Gets the best (lowest) exchange rate for a currency pair without caching.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Queries the database for the most recent rates</li>
     *   <li>Returns the best rate (lowest value) if multiple providers exist</li>
     *   <li>Does not update cache (use getExchangeRate for cached lookups)</li>
     * </ul>
     *
     * @param from the base currency code
     * @param to   the target currency code
     * @return the best exchange rate entity
     * @throws com.example.aidemo1.exception.ExchangeRateNotFoundException if no rate found
     */
    ExchangeRate getBestRate(String from, String to);

    /**
     * Fetches a fresh exchange rate from providers and updates cache/database.
     * 
     * <p>This method bypasses cache and database, forcing a fresh fetch from providers.</p>
     *
     * @param from the base currency code
     * @param to   the target currency code
     * @return the fetched exchange rate
     * @throws com.example.aidemo1.exception.ExternalProviderException if all providers fail
     */
    ExchangeRate fetchFreshRate(String from, String to);
}
