package com.example.aidemo1.service.impl;

import com.example.aidemo1.entity.Currency;
import com.example.aidemo1.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyServiceImpl.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyService Tests")
class CurrencyServiceImplTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private Currency sampleCurrency;

    @BeforeEach
    void setUp() {
        sampleCurrency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllCurrencies() Tests")
    class GetAllCurrenciesTests {

        @Test
        @DisplayName("Should return all currencies ordered by code")
        void shouldReturnAllCurrenciesOrderedByCode() {
            // Arrange
            var currency1 = Currency.builder().id(1L).code("EUR").name("Euro").build();
            var currency2 = Currency.builder().id(2L).code("USD").name("US Dollar").build();
            var expectedCurrencies = Arrays.asList(currency1, currency2);

            when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(expectedCurrencies);

            // Act
            var result = currencyService.getAllCurrencies();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(currency1, currency2);
            verify(currencyRepository).findAllByOrderByCodeAsc();
        }

        @Test
        @DisplayName("Should return empty list when no currencies exist")
        void shouldReturnEmptyListWhenNoCurrenciesExist() {
            // Arrange
            when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(Collections.emptyList());

            // Act
            var result = currencyService.getAllCurrencies();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
            verify(currencyRepository).findAllByOrderByCodeAsc();
        }
    }

    @Nested
    @DisplayName("addCurrency() Tests")
    class AddCurrencyTests {

        @Test
        @DisplayName("Should successfully add a new currency with valid code")
        void shouldSuccessfullyAddNewCurrency() {
            // Arrange
            var currencyCode = "USD";
            when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);
            when(currencyRepository.save(any(Currency.class))).thenReturn(sampleCurrency);

            // Act
            var result = currencyService.addCurrency(currencyCode);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("USD");
            assertThat(result.getName()).isEqualTo("US Dollar");
            verify(currencyRepository).existsByCode(currencyCode);
            verify(currencyRepository).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should normalize currency code to uppercase")
        void shouldNormalizeCurrencyCodeToUppercase() {
            // Arrange
            var lowercaseCode = "usd";
            when(currencyRepository.existsByCode("USD")).thenReturn(false);
            when(currencyRepository.save(any(Currency.class))).thenReturn(sampleCurrency);

            // Act
            var result = currencyService.addCurrency(lowercaseCode);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("USD");
            verify(currencyRepository).existsByCode("USD");
            verify(currencyRepository).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency already exists")
        void shouldThrowExceptionWhenCurrencyAlreadyExists() {
            // Arrange
            var currencyCode = "USD";
            when(currencyRepository.existsByCode(currencyCode)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency(currencyCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency already exists");
            verify(currencyRepository).existsByCode(currencyCode);
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code is null")
        void shouldThrowExceptionWhenCurrencyCodeIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code is empty")
        void shouldThrowExceptionWhenCurrencyCodeIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code is blank")
        void shouldThrowExceptionWhenCurrencyCodeIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null or empty");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code is too short")
        void shouldThrowExceptionWhenCurrencyCodeIsTooShort() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency("US"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be exactly 3 characters");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code is too long")
        void shouldThrowExceptionWhenCurrencyCodeIsTooLong() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency("USDD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be exactly 3 characters");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code contains numbers")
        void shouldThrowExceptionWhenCurrencyCodeContainsNumbers() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency("US1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only uppercase letters");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should throw exception when currency code contains special characters")
        void shouldThrowExceptionWhenCurrencyCodeContainsSpecialCharacters() {
            // Act & Assert
            assertThatThrownBy(() -> currencyService.addCurrency("US$"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only uppercase letters");
            verify(currencyRepository, never()).existsByCode(anyString());
            verify(currencyRepository, never()).save(any(Currency.class));
        }

        @Test
        @DisplayName("Should handle non-standard currency codes gracefully")
        void shouldHandleNonStandardCurrencyCodesGracefully() {
            // Arrange - XYZ is not a real ISO 4217 currency
            var nonStandardCode = "XYZ";
            var expectedCurrency = Currency.builder()
                    .id(1L)
                    .code(nonStandardCode)
                    .name(nonStandardCode) // Falls back to code when not recognized
                    .build();

            when(currencyRepository.existsByCode(nonStandardCode)).thenReturn(false);
            when(currencyRepository.save(any(Currency.class))).thenReturn(expectedCurrency);

            // Act
            var result = currencyService.addCurrency(nonStandardCode);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(nonStandardCode);
            verify(currencyRepository).save(any(Currency.class));
        }
    }

    @Nested
    @DisplayName("currencyExists() Tests")
    class CurrencyExistsTests {

        @Test
        @DisplayName("Should return true when currency exists")
        void shouldReturnTrueWhenCurrencyExists() {
            // Arrange
            var currencyCode = "USD";
            when(currencyRepository.existsByCode(currencyCode)).thenReturn(true);

            // Act
            var result = currencyService.currencyExists(currencyCode);

            // Assert
            assertThat(result).isTrue();
            verify(currencyRepository).existsByCode(currencyCode);
        }

        @Test
        @DisplayName("Should return false when currency does not exist")
        void shouldReturnFalseWhenCurrencyDoesNotExist() {
            // Arrange
            var currencyCode = "XYZ";
            when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);

            // Act
            var result = currencyService.currencyExists(currencyCode);

            // Assert
            assertThat(result).isFalse();
            verify(currencyRepository).existsByCode(currencyCode);
        }

        @Test
        @DisplayName("Should normalize code to uppercase before checking existence")
        void shouldNormalizeCodeToUppercaseBeforeCheckingExistence() {
            // Arrange
            var lowercaseCode = "usd";
            when(currencyRepository.existsByCode("USD")).thenReturn(true);

            // Act
            var result = currencyService.currencyExists(lowercaseCode);

            // Assert
            assertThat(result).isTrue();
            verify(currencyRepository).existsByCode("USD");
        }

        @Test
        @DisplayName("Should return false when currency code is null")
        void shouldReturnFalseWhenCurrencyCodeIsNull() {
            // Act
            var result = currencyService.currencyExists(null);

            // Assert
            assertThat(result).isFalse();
            verify(currencyRepository, never()).existsByCode(anyString());
        }

        @Test
        @DisplayName("Should return false when currency code is empty")
        void shouldReturnFalseWhenCurrencyCodeIsEmpty() {
            // Act
            var result = currencyService.currencyExists("");

            // Assert
            assertThat(result).isFalse();
            verify(currencyRepository, never()).existsByCode(anyString());
        }

        @Test
        @DisplayName("Should return false when currency code is blank")
        void shouldReturnFalseWhenCurrencyCodeIsBlank() {
            // Act
            var result = currencyService.currencyExists("   ");

            // Assert
            assertThat(result).isFalse();
            verify(currencyRepository, never()).existsByCode(anyString());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when repository is null")
        void shouldThrowExceptionWhenRepositoryIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new CurrencyServiceImpl(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CurrencyRepository must not be null");
        }
    }
}
