package com.example.aidemo1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an exchange rate between two currencies.
 * Stores rate information from different providers with timestamps.
 */
@Entity
@Table(name = "exchange_rate", indexes = {
    @Index(name = "idx_exchange_rate_currencies", columnList = "base_currency, target_currency"),
    @Index(name = "idx_exchange_rate_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_exchange_rate_period", columnList = "base_currency, target_currency, timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency code must be exactly 3 characters")
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @NotBlank(message = "Target currency is required")
    @Size(min = 3, max = 3, message = "Target currency code must be exactly 3 characters")
    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @NotNull(message = "Exchange rate is required")
    @Positive(message = "Exchange rate must be positive")
    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @NotBlank(message = "Provider is required")
    @Size(max = 50, message = "Provider name cannot exceed 50 characters")
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}