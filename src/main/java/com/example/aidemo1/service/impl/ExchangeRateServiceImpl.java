package com.example.aidemo1.service.impl;

import com.example.aidemo1.cache.RateCacheService;
import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.CurrencyNotFoundException;
import com.example.aidemo1.exception.ExchangeRateNotFoundException;
import com.example.aidemo1.integration.aggregator.RateAggregatorService;
import com.example.aidemo1.repository.CurrencyRepository;
import com.example.aidemo1.repository.ExchangeRateRepository;
import com.example.aidemo1.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of ExchangeRateService.
 * Manages exchange rate operations with a three-tier lookup strategy:
 * Cache → Database → External Providers.
 */
@Service
@Transactional(readOnly = true)
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private static final int RATE_FRESHNESS_HOURS = 1;
    private static final int DECIMAL_SCALE = 6;

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final RateCacheService rateCacheService;
    private final RateAggregatorService rateAggregatorService;

    /**
     * Constructor injection for all dependencies.
     *
     * @param exchangeRateRepository repository for exchange rate persistence
     * @param currencyRepository repository for currency validation
     * @param rateCacheService service for Redis cache operations
     * @param rateAggregatorService service for fetching rates from providers
     */
    public ExchangeRateServiceImpl(ExchangeRateRepository exchangeRateRepository,
                                    CurrencyRepository currencyRepository,
                                    RateCacheService rateCacheService,
                                    RateAggregatorService rateAggregatorService) {
        this.exchangeRateRepository = Objects.requireNonNull(exchangeRateRepository,
                "ExchangeRateRepository must not be null");
        this.currencyRepository = Objects.requireNonNull(currencyRepository,
                "CurrencyRepository must not be null");
        this.rateCacheService = Objects.requireNonNull(rateCacheService,
                "RateCacheService must not be null");
        this.rateAggregatorService = Objects.requireNonNull(rateAggregatorService,
                "RateAggregatorService must not be null");
    }

    @Override
    public BigDecimal getExchangeRate(String from, String to, BigDecimal amount) {
        logger.debug("Getting exchange rate: {} -> {} for amount {}", from, to, amount);

        // Validate inputs
        validateCurrencyCodes(from, to);
        validateAmount(amount);

        // Validate currencies exist
        validateCurrenciesExist(from, to);

        // Step 1: Check cache
        var cachedRate = rateCacheService.getCachedRate(from, to);
        if (cachedRate.isPresent()) {
            logger.info("Cache hit for {} -> {}", from, to);
            return calculateConvertedAmount(amount, cachedRate.get().getRate());
        }

        logger.debug("Cache miss for {} -> {}", from, to);

        // Step 2: Check database for recent rate
        var dbRate = findRecentRate(from, to);
        if (dbRate.isPresent()) {
            logger.info("Found recent rate in database for {} -> {}", from, to);
            var rate = dbRate.get();
            // Update cache with database rate
            rateCacheService.cacheRate(from, to, rate);
            return calculateConvertedAmount(amount, rate.getRate());
        }

        logger.debug("No recent rate in database for {} -> {}, fetching from providers", from, to);

        // Step 3: Fetch from providers
        var freshRate = fetchFreshRate(from, to);
        return calculateConvertedAmount(amount, freshRate.getRate());
    }

    @Override
    @Transactional
    public int refreshAllRates() {
        logger.info("Starting refresh of all exchange rates");

        // Get all currencies
        var currencies = currencyRepository.findAll();
        if (currencies.isEmpty()) {
            logger.warn("No currencies found in database, skipping refresh");
            return 0;
        }

        var currencyCodes = currencies.stream()
                .map(Currency::getCode)
                .toList();

        logger.info("Refreshing rates for {} currencies", currencyCodes.size());

        int successCount = 0;
        int totalPairs = 0;

        // Refresh all pairs (excluding same-currency pairs)
        for (String from : currencyCodes) {
            var targets = currencyCodes.stream()
                    .filter(to -> !to.equals(from))
                    .toList();

            var results = rateAggregatorService.fetchAndAggregateMultiple(from, targets);
            
            // Update cache for successful fetches
            results.forEach((to, rate) -> {
                rateCacheService.cacheRate(from, to, rate);
                logger.debug("Cached rate: {} -> {} = {}", from, to, rate.getRate());
            });

            successCount += results.size();
            totalPairs += targets.size();
        }

        logger.info("Refresh completed: {} out of {} currency pairs refreshed successfully",
                successCount, totalPairs);

        return successCount;
    }

    @Override
    public ExchangeRate getBestRate(String from, String to) {
        logger.debug("Getting best rate for {} -> {} from database", from, to);

        validateCurrencyCodes(from, to);

        // Get the most recent rate from database
        return exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(from, to)
                .orElseThrow(() -> {
                    logger.error("No exchange rate found in database for {} -> {}", from, to);
                    return ExchangeRateNotFoundException.forCurrencyPair(from, to);
                });
    }

    @Override
    @Transactional
    public ExchangeRate fetchFreshRate(String from, String to) {
        logger.info("Fetching fresh rate from providers: {} -> {}", from, to);

        validateCurrencyCodes(from, to);
        validateCurrenciesExist(from, to);

        // Fetch from providers (this will save to database automatically)
        var rate = rateAggregatorService.fetchAndAggregate(from, to);

        // Update cache
        rateCacheService.cacheRate(from, to, rate);

        logger.info("Successfully fetched and cached fresh rate: {} -> {} = {}",
                from, to, rate.getRate());

        return rate;
    }

    /**
     * Finds a recent exchange rate from the database.
     * A rate is considered recent if it was fetched within the last hour.
     *
     * @param from base currency code
     * @param to target currency code
     * @return Optional containing the rate if found and recent
     */
    private Optional<ExchangeRate> findRecentRate(String from, String to) {
        var cutoffTime = LocalDateTime.now().minusHours(RATE_FRESHNESS_HOURS);

        return exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(from, to)
                .filter(rate -> rate.getTimestamp().isAfter(cutoffTime));
    }

    /**
     * Calculates the converted amount using the exchange rate.
     *
     * @param amount the amount to convert
     * @param rate the exchange rate
     * @return the converted amount with proper decimal scale
     */
    private BigDecimal calculateConvertedAmount(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Validates both currency codes.
     *
     * @param from base currency code
     * @param to target currency code
     * @throws IllegalArgumentException if either code is invalid
     */
    private void validateCurrencyCodes(String from, String to) {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Base currency code must not be null or empty");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Target currency code must not be null or empty");
        }
        if (from.length() != 3 || to.length() != 3) {
            throw new IllegalArgumentException("Currency codes must be exactly 3 characters (ISO 4217)");
        }
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Base and target currencies must be different");
        }
    }

    /**
     * Validates that both currencies exist in the database.
     *
     * @param from base currency code
     * @param to target currency code
     * @throws CurrencyNotFoundException if either currency doesn't exist
     */
    private void validateCurrenciesExist(String from, String to) {
        if (!currencyRepository.existsByCode(from)) {
            throw CurrencyNotFoundException.forCode(from);
        }
        if (!currencyRepository.existsByCode(to)) {
            throw CurrencyNotFoundException.forCode(to);
        }
    }

    /**
     * Validates the amount to convert.
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is invalid
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
