package com.example.aidemo1.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request object for adding a new currency to the system")
public class AddCurrencyRequest {

    /**
     * Currency code (ISO 4217 format, 3 uppercase letters).
     * Example: USD, EUR, GBP
     */
    @Schema(
            description = "Currency code in ISO 4217 format (3 uppercase letters)",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED,
            pattern = "^[A-Z]{3}$",
            minLength = 3,
            maxLength = 3
    )
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters (ISO 4217)")
    private String code;

    /**
     * Optional human-readable name for the currency.
     * Example: "US Dollar", "Euro", "British Pound"
     */
    @Schema(
            description = "Human-readable name of the currency",
            example = "US Dollar",
            maxLength = 100
    )
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String name;
}
