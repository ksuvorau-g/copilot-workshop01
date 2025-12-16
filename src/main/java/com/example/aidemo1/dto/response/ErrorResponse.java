package com.example.aidemo1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response DTO for consistent error handling across the API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response object for all API errors")
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    @Schema(
            description = "Timestamp when the error occurred",
            example = "2025-10-15T14:30:00"
    )
    private LocalDateTime timestamp;

    /**
     * HTTP status code.
     * Example: 404, 400, 500
     */
    @Schema(
            description = "HTTP status code",
            example = "400"
    )
    private Integer status;

    /**
     * Short error type or title.
     * Example: "Not Found", "Bad Request", "Internal Server Error"
     */
    @Schema(
            description = "Short error type or category",
            example = "Bad Request"
    )
    private String error;

    /**
     * Detailed error message.
     * Example: "Currency with code XYZ not found"
     */
    @Schema(
            description = "Detailed error message explaining what went wrong",
            example = "Currency with code XYZ not found"
    )
    private String message;

    /**
     * Request path where the error occurred.
     * Example: "/api/v1/currencies/exchange-rates"
     */
    @Schema(
            description = "Request path where the error occurred",
            example = "/api/v1/currencies/exchange-rates"
    )
    private String path;

    /**
     * List of validation errors (for 400 Bad Request with multiple validation failures).
     * Example: ["Currency code must be 3 uppercase letters", "Amount must be positive"]
     */
    @Schema(
            description = "List of validation errors (present when there are multiple validation failures)",
            example = "[\"Currency code must be 3 uppercase letters\", \"Amount must be positive\"]"
    )
    private List<String> validationErrors;

    /**
     * Additional debug information (typically only in dev/test environments).
     * Can include stack traces, request details, etc.
     */
    @Schema(
            description = "Additional debug information (only in development/test environments)"
    )
    private Map<String, Object> debugInfo;

    /**
     * Creates a simple error response with just status, error type, and message.
     */
    public static ErrorResponse of(Integer status, String error, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .build();
    }

    /**
     * Creates an error response with path information.
     */
    public static ErrorResponse of(Integer status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates an error response with validation errors.
     */
    public static ErrorResponse withValidationErrors(Integer status, String error, String message, 
                                                      String path, List<String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }
}
