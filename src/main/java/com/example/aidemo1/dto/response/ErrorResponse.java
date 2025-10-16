package com.example.aidemo1.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    private LocalDateTime timestamp;

    /**
     * HTTP status code.
     * Example: 404, 400, 500
     */
    private Integer status;

    /**
     * Short error type or title.
     * Example: "Not Found", "Bad Request", "Internal Server Error"
     */
    private String error;

    /**
     * Detailed error message.
     * Example: "Currency with code XYZ not found"
     */
    private String message;

    /**
     * Request path where the error occurred.
     * Example: "/api/v1/currencies/exchange-rates"
     */
    private String path;

    /**
     * List of validation errors (for 400 Bad Request with multiple validation failures).
     * Example: ["Currency code must be 3 uppercase letters", "Amount must be positive"]
     */
    private List<String> validationErrors;

    /**
     * Additional debug information (typically only in dev/test environments).
     * Can include stack traces, request details, etc.
     */
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
