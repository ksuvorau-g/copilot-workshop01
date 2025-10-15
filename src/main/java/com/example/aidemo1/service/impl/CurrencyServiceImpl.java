package com.example.aidemo1.service.impl;

import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.repository.CurrencyRepository;
import com.example.aidemo1.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Implementation of CurrencyService.
 * Manages currency operations including validation and database interactions.
 */
@Service
@Transactional(readOnly = true)
public class CurrencyServiceImpl implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final int CURRENCY_CODE_LENGTH = 3;

    private final CurrencyRepository currencyRepository;

    /**
     * Constructor injection for dependencies.
     *
     * @param currencyRepository the currency repository
     */
    public CurrencyServiceImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = Objects.requireNonNull(currencyRepository, "CurrencyRepository must not be null");
    }

    @Override
    public List<Currency> getAllCurrencies() {
        logger.debug("Fetching all currencies from database");
        var currencies = currencyRepository.findAllByOrderByCodeAsc();
        logger.info("Retrieved {} currencies", currencies.size());
        return currencies;
    }

    @Override
    @Transactional
    public Currency addCurrency(String currencyCode) {
        logger.debug("Adding new currency with code: {}", currencyCode);

        // Validate input
        validateCurrencyCode(currencyCode);

        // Normalize to uppercase
        var normalizedCode = currencyCode.toUpperCase(Locale.ROOT);

        // Check if currency already exists
        if (currencyRepository.existsByCode(normalizedCode)) {
            logger.warn("Currency already exists: {}", normalizedCode);
            throw new IllegalArgumentException("Currency already exists: " + normalizedCode);
        }

        // Create and save currency
        var currency = Currency.builder()
                .code(normalizedCode)
                .name(getCurrencyName(normalizedCode))
                .build();

        var savedCurrency = currencyRepository.save(currency);
        logger.info("Successfully added currency: {}", savedCurrency.getCode());

        return savedCurrency;
    }

    @Override
    public boolean currencyExists(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return false;
        }

        var normalizedCode = currencyCode.toUpperCase(Locale.ROOT);
        var exists = currencyRepository.existsByCode(normalizedCode);
        logger.debug("Currency {} exists: {}", normalizedCode, exists);

        return exists;
    }

    /**
     * Validates a currency code according to ISO 4217 format.
     * Must be exactly 3 uppercase letters.
     *
     * @param currencyCode the currency code to validate
     * @throws IllegalArgumentException if the currency code is invalid
     */
    private void validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("Currency code must not be null or empty");
        }

        var normalizedCode = currencyCode.toUpperCase(Locale.ROOT);

        if (normalizedCode.length() != CURRENCY_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "Currency code must be exactly 3 characters, got: " + currencyCode.length());
        }

        if (!CURRENCY_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException(
                    "Currency code must contain only uppercase letters, got: " + currencyCode);
        }
    }

    /**
     * Gets a human-readable name for a currency code.
     * Uses Java's Currency class to retrieve standard names.
     * Falls back to the code itself if the currency is not recognized.
     *
     * @param currencyCode the currency code
     * @return the currency name
     */
    private String getCurrencyName(String currencyCode) {
        try {
            var javaCurrency = java.util.Currency.getInstance(currencyCode);
            return javaCurrency.getDisplayName(Locale.ENGLISH);
        } catch (IllegalArgumentException e) {
            logger.warn("Currency code {} not recognized by Java Currency API, using code as name", currencyCode);
            return currencyCode;
        }
    }
}
