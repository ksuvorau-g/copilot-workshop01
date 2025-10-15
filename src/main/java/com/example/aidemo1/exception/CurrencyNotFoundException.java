package com.example.aidemo1.exception;

/**
 * Exception thrown when a currency is not found in the database.
 * This is a runtime exception that should be caught by global exception handlers.
 */
public class CurrencyNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Currency not found";

    /**
     * Constructs a new CurrencyNotFoundException with a default message.
     */
    public CurrencyNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new CurrencyNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public CurrencyNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new CurrencyNotFoundException with a message including the currency code.
     *
     * @param currencyCode the currency code that was not found
     * @return the constructed exception
     */
    public static CurrencyNotFoundException forCode(String currencyCode) {
        return new CurrencyNotFoundException("Currency not found: " + currencyCode);
    }

    /**
     * Constructs a new CurrencyNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public CurrencyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
