package com.example.aidemo1.controller.mock;

import com.example.aidemo1.integration.dto.external.MockProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;

/**
 * Mock Exchange Rate Provider 1 REST Controller.
 * 
 * <p>This controller simulates an external exchange rate provider API for testing
 * and development purposes. It generates random exchange rates within a realistic
 * range to mimic real provider behavior.</p>
 * 
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>GET /mock/provider1/rate - Get random exchange rate for a currency pair</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This is for development/testing only and should not be
 * used in production environments.</p>
 */
@Slf4j
@RestController
@RequestMapping("/mock/provider1")
@Tag(name = "Mock Provider 1", description = "Internal mock exchange rate provider for testing")
public class MockProvider1Controller {
    
    private static final String PROVIDER_NAME = "Mock Provider 1";
    private final Random random = new Random();
    
    /**
     * Get a random exchange rate for the specified currency pair.
     * 
     * <p>This endpoint simulates an external provider's rate endpoint by generating
     * a random rate within a typical range (0.5 to 2.0). The rate is scaled and
     * rounded to 6 decimal places for realism.</p>
     * 
     * @param base the base currency code (e.g., "USD")
     * @param target the target currency code (e.g., "EUR")
     * @return response containing the generated exchange rate
     */
    @Operation(
            summary = "Get mock exchange rate",
            description = "Returns a randomly generated exchange rate for the specified currency pair. " +
                         "Rates are generated within a realistic range (0.5 to 2.0) for testing purposes."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully generated mock exchange rate",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MockProviderResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid currency codes provided"
    )
    @GetMapping("/rate")
    public ResponseEntity<MockProviderResponse> getExchangeRate(
            @Parameter(description = "Base currency code (e.g., USD)", required = true)
            @RequestParam String base,
            
            @Parameter(description = "Target currency code (e.g., EUR)", required = true)
            @RequestParam String target
    ) {
        log.debug("Mock Provider 1 received request: {} -> {}", base, target);
        
        if (base == null || base.isBlank() || target == null || target.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(MockProviderResponse.builder()
                            .success(false)
                            .error("Base and target currency codes are required")
                            .build());
        }
        
        // Generate a random rate between 0.5 and 2.0
        BigDecimal rate = BigDecimal.valueOf(0.5 + (random.nextDouble() * 1.5))
                .setScale(6, RoundingMode.HALF_UP);
        
        MockProviderResponse response = MockProviderResponse.builder()
                .success(true)
                .base(base.toUpperCase())
                .target(target.toUpperCase())
                .rate(rate)
                .timestamp(Instant.now().getEpochSecond())
                .provider(PROVIDER_NAME)
                .build();
        
        log.info("Mock Provider 1 generated rate: {} -> {} = {}", base, target, rate);
        
        return ResponseEntity.ok(response);
    }
}
