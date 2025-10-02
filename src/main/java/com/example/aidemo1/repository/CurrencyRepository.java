package com.example.aidemo1.repository;

import com.example.aidemo1.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Currency entity.
 * Provides CRUD operations and custom queries for currency management.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /**
     * Find a currency by its code (e.g., USD, EUR).
     *
     * @param code the currency code (3 characters, ISO 4217)
     * @return Optional containing the currency if found
     */
    Optional<Currency> findByCode(String code);

    /**
     * Check if a currency exists by its code.
     *
     * @param code the currency code
     * @return true if currency exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Find a currency by code, ignoring case.
     *
     * @param code the currency code
     * @return Optional containing the currency if found
     */
    Optional<Currency> findByCodeIgnoreCase(String code);

    /**
     * Delete a currency by its code.
     *
     * @param code the currency code
     */
    void deleteByCode(String code);

    /**
     * Get all currencies ordered by code.
     *
     * @return list of all currencies sorted by code
     */
    List<Currency> findAllByOrderByCodeAsc();
}
