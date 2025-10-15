package com.example.aidemo1.exception;

/**
 * Exception thrown when an exchange rate is not found in the cache, database, or providers.
 * This is a runtime exception that should be caught by global exception handlers.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Exchange rate not found";

    /**
     * Constructs a new ExchangeRateNotFoundException with a default message.
     */
    public ExchangeRateNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new ExchangeRateNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExchangeRateNotFoundException for a currency pair.
     *
     * @param from the base currency code
     * @param to   the target currency code
     * @return the constructed exception
     */
    public static ExchangeRateNotFoundException forCurrencyPair(String from, String to) {
        return new ExchangeRateNotFoundException(
                String.format("Exchange rate not found for currency pair: %s -> %s", from, to));
    }

    /**
     * Constructs a new ExchangeRateNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public ExchangeRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
