package com.example.aidemo1.controller.api.v1;

import com.example.aidemo1.dto.request.AddCurrencyRequest;
import com.example.aidemo1.dto.response.CurrencyResponse;
import com.example.aidemo1.dto.response.ExchangeRateResponse;
import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.entity.ExchangeRate;
import com.example.aidemo1.service.CurrencyService;
import com.example.aidemo1.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for currency and exchange rate operations.
 * Provides endpoints for managing currencies and fetching exchange rates.
 * 
 * <p>Base URL: /api/v1/currencies</p>
 * 
 * <p>Security:</p>
 * <ul>
 *   <li>GET endpoints are public (configured in SecurityConfig)</li>
 *   <li>POST endpoints require authentication (to be implemented)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    /**
     * Get all currencies.
     * 
     * <p>Public endpoint - no authentication required.</p>
     * 
     * @return list of all currencies
     */
    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        log.info("GET /api/v1/currencies - Fetching all currencies");
        
        List<Currency> currencies = currencyService.getAllCurrencies();
        List<CurrencyResponse> response = currencies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        log.info("GET /api/v1/currencies - Returning {} currencies", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get exchange rate for a currency pair and calculate converted amount.
     * 
     * <p>Public endpoint - no authentication required.</p>
     * 
     * <p>Request parameters:</p>
     * <ul>
     *   <li>from - Source currency code (3 letters, e.g., USD)</li>
     *   <li>to - Target currency code (3 letters, e.g., EUR)</li>
     *   <li>amount - Amount to convert (positive number)</li>
     * </ul>
     * 
     * <p>Example: GET /api/v1/currencies/exchange-rates?from=USD&to=EUR&amount=100</p>
     *
     * @param from source currency code
     * @param to target currency code
     * @param amount amount to convert
     * @return exchange rate information with converted amount
     */
    @GetMapping("/exchange-rates")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        
        log.info("GET /api/v1/currencies/exchange-rates - from={}, to={}, amount={}", 
                from, to, amount);

        // Validate input using manual validation (since we're using query params)
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("Source currency (from) is required");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Target currency (to) is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        // Normalize currency codes to uppercase
        String fromUpper = from.trim().toUpperCase();
        String toUpper = to.trim().toUpperCase();
        
        // Get the best rate for this currency pair
        ExchangeRate bestRate = exchangeRateService.getBestRate(fromUpper, toUpper);
        
        // Calculate converted amount
        BigDecimal convertedAmount = amount.multiply(bestRate.getRate())
                .setScale(2, RoundingMode.HALF_UP);
        
        // Build response
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .from(fromUpper)
                .to(toUpper)
                .amount(amount)
                .rate(bestRate.getRate())
                .convertedAmount(convertedAmount)
                .provider(bestRate.getProvider())
                .timestamp(bestRate.getTimestamp())
                .build();
        
        log.info("GET /api/v1/currencies/exchange-rates - Successfully calculated: {} {} = {} {} (rate: {}, provider: {})",
                amount, fromUpper, convertedAmount, toUpper, bestRate.getRate(), bestRate.getProvider());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new currency to the system.
     * 
     * <p>Restricted endpoint - requires ADMIN role.</p>
     * 
     * <p>Request body example:</p>
     * <pre>
     * {
     *   "code": "USD",
     *   "name": "US Dollar"
     * }
     * </pre>
     *
     * @param request the currency to add
     * @return the created currency with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CurrencyResponse> addCurrency(@Valid @RequestBody AddCurrencyRequest request) {
        log.info("POST /api/v1/currencies - Adding new currency: code={}, name={}", 
                request.getCode(), request.getName());
        
        // Add currency through service (includes validation)
        Currency currency = currencyService.addCurrency(request.getCode());
        
        // Map to response
        CurrencyResponse response = mapToResponse(currency);
        
        log.info("POST /api/v1/currencies - Successfully added currency: {}", response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh all exchange rates by fetching fresh data from providers.
     * 
     * <p>Restricted endpoint - requires ADMIN role.</p>
     * 
     * <p>This endpoint:</p>
     * <ul>
     *   <li>Fetches rates from all configured providers</li>
     *   <li>Saves all rates to the database</li>
     *   <li>Updates Redis cache with best rates</li>
     *   <li>Returns the count of successfully refreshed currency pairs</li>
     * </ul>
     * 
     * <p>Response example:</p>
     * <pre>
     * {
     *   "message": "Successfully refreshed exchange rates",
     *   "refreshedPairs": 12,
     *   "timestamp": "2025-10-15T13:45:00"
     * }
     * </pre>
     *
     * @return success message with count of refreshed pairs
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshRates() {
        log.info("POST /api/v1/currencies/refresh - Triggering manual rate refresh");
        
        // Refresh all rates
        int refreshedCount = exchangeRateService.refreshAllRates();
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully refreshed exchange rates");
        response.put("refreshedPairs", refreshedCount);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        log.info("POST /api/v1/currencies/refresh - Successfully refreshed {} currency pairs", refreshedCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to map Currency entity to CurrencyResponse DTO.
     *
     * @param currency the currency entity
     * @return the currency response DTO
     */
    private CurrencyResponse mapToResponse(Currency currency) {
        return CurrencyResponse.builder()
                .code(currency.getCode())
                .name(currency.getName())
                .createdAt(currency.getCreatedAt())
                .updatedAt(currency.getUpdatedAt())
                .build();
    }
}
