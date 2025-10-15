package com.example.aidemo1.service;

import com.example.aidemo1.entity.Currency;

import java.util.List;

/**
 * Service interface for currency operations.
 * Provides business logic for managing currencies.
 */
public interface CurrencyService {

    /**
     * Retrieves all currencies from the database.
     *
     * @return list of all currencies, ordered by currency code
     */
    List<Currency> getAllCurrencies();

    /**
     * Adds a new currency to the database.
     * Validates the currency code format (3 uppercase letters, ISO 4217).
     * Checks if the currency already exists to prevent duplicates.
     *
     * @param currencyCode the 3-letter currency code (e.g., USD, EUR)
     * @return the newly created currency
     * @throws IllegalArgumentException if the currency code is invalid or already exists
     */
    Currency addCurrency(String currencyCode);

    /**
     * Checks if a currency exists in the database.
     *
     * @param currencyCode the currency code to check
     * @return true if the currency exists, false otherwise
     */
    boolean currencyExists(String currencyCode);
}
