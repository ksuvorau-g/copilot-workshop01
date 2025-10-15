package com.example.aidemo1.integration.aggregator;

import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.exception.ExternalProviderException;
import com.example.aidemo1.integration.provider.ExchangeRateProvider;
import com.example.aidemo1.repository.ExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for aggregating exchange rates from multiple providers.
 * 
 * <p>This service fetches rates from all available exchange rate providers,
 * selects the best rate based on business logic (lowest rate and highest priority),
 * and persists all rates to the database for historical tracking.</p>
 * 
 * <h2>Best Rate Selection Logic</h2>
 * <p>The best rate is determined by:</p>
 * <ol>
 *   <li>Primary criterion: Lowest exchange rate value (best deal for conversion)</li>
 *   <li>Tie-breaker: Highest provider priority (prefer real providers over mocks)</li>
 * </ol>
 * 
 * <h2>Error Handling</h2>
 * <p>If a provider fails, the aggregator logs the error and continues with
 * other providers. If all providers fail, an exception is thrown.</p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Fetch from all providers and get the best rate
 * ExchangeRate bestRate = rateAggregator.fetchAndAggregate("USD", "EUR");
 * System.out.println("Best rate: " + bestRate.getRate() + " from " + bestRate.getProvider());
 * 
 * // Fetch and persist rates for all currency pairs
 * rateAggregator.refreshAllRates(List.of("USD", "EUR", "GBP"));
 * }</pre>
 * 
 * @see ExchangeRateProvider
 * @see ExchangeRate
 */
@Slf4j
@Service
public class RateAggregatorService {
    
    private final List<ExchangeRateProvider> providers;
    private final ExchangeRateRepository exchangeRateRepository;
    
    /**
     * Constructs the rate aggregator with all available providers.
     * 
     * <p>Spring auto-wires all beans implementing {@link ExchangeRateProvider},
     * allowing dynamic provider registration without code changes.</p>
     * 
     * @param providers list of all available exchange rate providers
     * @param exchangeRateRepository repository for persisting rates
     */
    public RateAggregatorService(List<ExchangeRateProvider> providers,
                                  ExchangeRateRepository exchangeRateRepository) {
        this.providers = providers;
        this.exchangeRateRepository = exchangeRateRepository;
        log.info("RateAggregatorService initialized with {} providers: {}", 
                providers.size(), 
                providers.stream()
                        .map(ExchangeRateProvider::getProviderName)
                        .collect(Collectors.joining(", ")));
    }
    
    /**
     * Fetches exchange rates from all available providers and returns the best one.
     * 
     * <p>This method queries all providers that support the given currency pair,
     * collects their rates, persists all rates to the database, and returns the
     * best rate according to the selection logic.</p>
     * 
     * @param from the base currency code (e.g., "USD")
     * @param to the target currency code (e.g., "EUR")
     * @return the best exchange rate from all providers
     * @throws ExternalProviderException if all providers fail
     * @throws IllegalArgumentException if currency codes are invalid
     */
    @Transactional
    public ExchangeRate fetchAndAggregate(String from, String to) {
        validateCurrencyCodes(from, to);
        
        log.info("Aggregating rates for {} -> {} from {} providers", from, to, providers.size());
        
        // Filter providers that support this currency pair
        List<ExchangeRateProvider> supportingProviders = providers.stream()
                .filter(provider -> provider.supports(from, to))
                .sorted(Comparator.comparingInt(ExchangeRateProvider::getPriority).reversed())
                .collect(Collectors.toList());
        
        if (supportingProviders.isEmpty()) {
            throw new ExternalProviderException(
                    String.format("No providers support currency pair %s -> %s", from, to));
        }
        
        log.debug("Found {} providers supporting {} -> {}: {}", 
                supportingProviders.size(), from, to,
                supportingProviders.stream()
                        .map(ExchangeRateProvider::getProviderName)
                        .collect(Collectors.joining(", ")));
        
        // Fetch rates from all supporting providers
        List<ExchangeRate> fetchedRates = new ArrayList<>();
        Map<String, Exception> failures = new HashMap<>();
        
        for (ExchangeRateProvider provider : supportingProviders) {
            try {
                log.debug("Fetching rate from provider: {}", provider.getProviderName());
                ExchangeRate rate = provider.fetchRate(from, to);
                fetchedRates.add(rate);
                log.debug("Successfully fetched rate from {}: {}", 
                        provider.getProviderName(), rate.getRate());
            } catch (Exception e) {
                log.warn("Provider {} failed for {} -> {}: {}", 
                        provider.getProviderName(), from, to, e.getMessage());
                failures.put(provider.getProviderName(), e);
            }
        }
        
        // Check if we got any rates
        if (fetchedRates.isEmpty()) {
            String errorDetails = failures.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue().getMessage())
                    .collect(Collectors.joining("; "));
            throw new ExternalProviderException(
                    String.format("All providers failed for %s -> %s. Failures: %s", 
                            from, to, errorDetails));
        }
        
        // Save all fetched rates to database
        List<ExchangeRate> savedRates = exchangeRateRepository.saveAll(fetchedRates);
        log.info("Saved {} rates to database for {} -> {}", savedRates.size(), from, to);
        
        // Select and return the best rate
        ExchangeRate bestRate = selectBestRate(savedRates);
        log.info("Best rate for {} -> {}: {} from {} (priority: {})", 
                from, to, bestRate.getRate(), bestRate.getProvider(),
                getBestRateProvider(bestRate, supportingProviders).map(ExchangeRateProvider::getPriority).orElse(-1));
        
        return bestRate;
    }
    
    /**
     * Fetches and aggregates rates for multiple currency pairs.
     * 
     * <p>This method is useful for batch operations, such as refreshing
     * all supported currency pairs during scheduled updates.</p>
     * 
     * @param from the base currency code
     * @param targets list of target currency codes
     * @return map of target currency to best exchange rate
     */
    @Transactional
    public Map<String, ExchangeRate> fetchAndAggregateMultiple(String from, List<String> targets) {
        validateCurrencyCode(from, "Base currency");
        
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Target currencies list cannot be null or empty");
        }
        
        log.info("Aggregating rates for {} -> {} targets", from, targets.size());
        
        Map<String, ExchangeRate> results = new HashMap<>();
        Map<String, Exception> failures = new HashMap<>();
        
        for (String target : targets) {
            try {
                ExchangeRate rate = fetchAndAggregate(from, target);
                results.put(target, rate);
            } catch (Exception e) {
                log.error("Failed to aggregate rate for {} -> {}: {}", from, target, e.getMessage());
                failures.put(target, e);
            }
        }
        
        log.info("Successfully aggregated {} out of {} currency pairs", 
                results.size(), targets.size());
        
        if (!failures.isEmpty()) {
            log.warn("Failed to aggregate {} currency pairs: {}", 
                    failures.size(), 
                    String.join(", ", failures.keySet()));
        }
        
        return results;
    }
    
    /**
     * Fetches rates from all providers without persisting (useful for testing).
     * 
     * @param from the base currency code
     * @param to the target currency code
     * @return list of all successfully fetched rates
     */
    public List<ExchangeRate> fetchFromAllProviders(String from, String to) {
        validateCurrencyCodes(from, to);
        
        return providers.stream()
                .filter(provider -> provider.supports(from, to))
                .map(provider -> {
                    try {
                        return provider.fetchRate(from, to);
                    } catch (Exception e) {
                        log.warn("Provider {} failed: {}", provider.getProviderName(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Selects the best rate from a list of rates.
     * 
     * <p>Selection logic:</p>
     * <ol>
     *   <li>Primary: Choose the lowest rate (best deal for the customer)</li>
     *   <li>Tie-breaker: If rates are equal, prefer higher priority provider</li>
     * </ol>
     * 
     * @param rates list of exchange rates to choose from
     * @return the best exchange rate
     */
    private ExchangeRate selectBestRate(List<ExchangeRate> rates) {
        if (rates == null || rates.isEmpty()) {
            throw new IllegalArgumentException("Cannot select best rate from empty list");
        }
        
        if (rates.size() == 1) {
            return rates.get(0);
        }
        
        // Sort by rate (ascending), then by provider priority (descending)
        return rates.stream()
                .min(Comparator
                        .comparing(ExchangeRate::getRate)
                        .thenComparing((rate1, rate2) -> {
                            // For tie-breaking, prefer higher priority providers
                            int priority1 = getProviderPriority(rate1.getProvider());
                            int priority2 = getProviderPriority(rate2.getProvider());
                            return Integer.compare(priority2, priority1); // Descending order
                        }))
                .orElseThrow(() -> new IllegalStateException("Failed to select best rate"));
    }
    
    /**
     * Gets the priority of a provider by its name.
     * 
     * @param providerName the name of the provider
     * @return the provider's priority, or 0 if not found
     */
    private int getProviderPriority(String providerName) {
        return providers.stream()
                .filter(p -> p.getProviderName().equals(providerName))
                .findFirst()
                .map(ExchangeRateProvider::getPriority)
                .orElse(0);
    }
    
    /**
     * Gets the provider instance for a given rate.
     * 
     * @param rate the exchange rate
     * @param availableProviders list of providers to search
     * @return Optional containing the provider if found
     */
    private Optional<ExchangeRateProvider> getBestRateProvider(
            ExchangeRate rate, List<ExchangeRateProvider> availableProviders) {
        return availableProviders.stream()
                .filter(p -> p.getProviderName().equals(rate.getProvider()))
                .findFirst();
    }
    
    /**
     * Validates both currency codes.
     * 
     * @param from the base currency code
     * @param to the target currency code
     * @throws IllegalArgumentException if either code is invalid
     */
    private void validateCurrencyCodes(String from, String to) {
        validateCurrencyCode(from, "Base currency");
        validateCurrencyCode(to, "Target currency");
        
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException(
                    "Base and target currencies must be different");
        }
    }
    
    /**
     * Validates a single currency code.
     * 
     * @param code the currency code to validate
     * @param fieldName the name of the field (for error messages)
     * @throws IllegalArgumentException if the code is invalid
     */
    private void validateCurrencyCode(String code, String fieldName) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        
        if (code.length() != 3) {
            throw new IllegalArgumentException(
                    fieldName + " must be exactly 3 characters (ISO 4217)");
        }
    }
    
    /**
     * Returns the number of active providers.
     * 
     * @return the count of providers
     */
    public int getProviderCount() {
        return providers.size();
    }
    
    /**
     * Returns the list of provider names.
     * 
     * @return list of provider names
     */
    public List<String> getProviderNames() {
        return providers.stream()
                .map(ExchangeRateProvider::getProviderName)
                .collect(Collectors.toList());
    }
}
