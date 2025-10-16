package com.example.aidemo1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a new currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCurrencyRequest {

    /**
     * Currency code (ISO 4217 format, 3 uppercase letters).
     * Example: USD, EUR, GBP
     */
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    private String code;

    /**
     * Optional human-readable name for the currency.
     * Example: "US Dollar", "Euro", "British Pound"
     */
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String name;
}
