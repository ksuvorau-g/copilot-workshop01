package com.example.aidemo1.repository;

import com.example.aidemo1.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExchangeRate entity.
 * Provides CRUD operations and custom queries for exchange rate management.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Find the most recent exchange rate for a currency pair.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the most recent rate if found
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("base") String baseCurrency, 
                                          @Param("target") String targetCurrency);

    /**
     * Find all exchange rates for a currency pair within a time range.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of exchange rates in the time range, ordered by timestamp descending
     */
    List<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndTimestampBetweenOrderByTimestampDesc(
        String baseCurrency,
        String targetCurrency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * Find all exchange rates for a currency pair since a specific time.
     * Used for historical analysis and trend calculations.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @param since the start time
     * @return list of exchange rates since the specified time, ordered by timestamp ascending
     */
    List<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndTimestampGreaterThanEqualOrderByTimestampAsc(
        String baseCurrency,
        String targetCurrency,
        LocalDateTime since
    );

    /**
     * Find all rates from a specific provider for a currency pair.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @param provider the provider name
     * @return list of rates from the specified provider
     */
    List<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndProvider(
        String baseCurrency,
        String targetCurrency,
        String provider
    );

    /**
     * Find the best (lowest) rate for a currency pair from all providers at the latest timestamp.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the best rate if found
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.targetCurrency = :target " +
           "AND e.timestamp = (SELECT MAX(e2.timestamp) FROM ExchangeRate e2 WHERE e2.baseCurrency = :base AND e2.targetCurrency = :target) " +
           "ORDER BY e.rate ASC LIMIT 1")
    Optional<ExchangeRate> findBestRate(@Param("base") String baseCurrency,
                                       @Param("target") String targetCurrency);

    /**
     * Find all rates for a currency pair, ordered by timestamp descending.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return list of all rates for the currency pair, most recent first
     */
    List<ExchangeRate> findByBaseCurrencyAndTargetCurrencyOrderByTimestampDesc(
        String baseCurrency,
        String targetCurrency
    );

    /**
     * Delete all rates older than a specified time.
     * Useful for cleaning up old historical data.
     *
     * @param cutoffTime the cutoff time
     */
    void deleteByTimestampBefore(LocalDateTime cutoffTime);

    /**
     * Count exchange rates for a specific currency pair.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return count of rates
     */
    long countByBaseCurrencyAndTargetCurrency(String baseCurrency, String targetCurrency);

    /**
     * Find all unique currency pairs that have exchange rates.
     *
     * @return list of unique currency pair combinations as Object arrays [baseCurrency, targetCurrency]
     */
    @Query("SELECT DISTINCT e.baseCurrency, e.targetCurrency FROM ExchangeRate e")
    List<Object[]> findAllUniqueCurrencyPairs();

    /**
     * Find the first (oldest) exchange rate for a currency pair.
     *
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the oldest rate if found
     */
    Optional<ExchangeRate> findFirstByBaseCurrencyAndTargetCurrencyOrderByTimestampAsc(
        String baseCurrency,
        String targetCurrency
    );
}
