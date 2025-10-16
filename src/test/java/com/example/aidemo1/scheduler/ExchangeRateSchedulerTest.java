package com.example.aidemo1.scheduler;

import com.example.aidemo1.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateScheduler.
 * 
 * Tests verify scheduled task execution, error handling, and manual trigger functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateScheduler Tests")
class ExchangeRateSchedulerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateScheduler scheduler;

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create scheduler with valid ExchangeRateService")
        void shouldCreateSchedulerWithValidService() {
            // Given
            ExchangeRateService service = mock(ExchangeRateService.class);

            // When
            ExchangeRateScheduler newScheduler = new ExchangeRateScheduler(service);

            // Then
            assertThat(newScheduler).isNotNull();
        }
    }

    @Nested
    @DisplayName("refreshExchangeRates Tests")
    class RefreshExchangeRatesTests {

        @Test
        @DisplayName("Should successfully refresh exchange rates")
        void shouldSuccessfullyRefreshExchangeRates() {
            // Given
            int expectedRefreshCount = 6;
            when(exchangeRateService.refreshAllRates()).thenReturn(expectedRefreshCount);

            // When
            scheduler.refreshExchangeRates();

            // Then
            verify(exchangeRateService, times(1)).refreshAllRates();
        }

        @Test
        @DisplayName("Should log success when refresh completes without errors")
        void shouldLogSuccessWhenRefreshCompletes() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(10);

            // When
            scheduler.refreshExchangeRates();

            // Then
            verify(exchangeRateService).refreshAllRates();
            // In real scenario, verify logs using log capturing framework
        }

        @Test
        @DisplayName("Should handle exception gracefully and not rethrow")
        void shouldHandleExceptionGracefully() {
            // Given
            RuntimeException testException = new RuntimeException("Provider connection failed");
            when(exchangeRateService.refreshAllRates()).thenThrow(testException);

            // When & Then - should not throw exception
            assertThatCode(() -> scheduler.refreshExchangeRates())
                    .doesNotThrowAnyException();

            verify(exchangeRateService).refreshAllRates();
        }

        @Test
        @DisplayName("Should continue after exception for next scheduled run")
        void shouldContinueAfterException() {
            // Given
            when(exchangeRateService.refreshAllRates())
                    .thenThrow(new RuntimeException("First call fails"))
                    .thenReturn(5); // Second call succeeds

            // When - first call fails
            scheduler.refreshExchangeRates();

            // Then - verify first call was made
            verify(exchangeRateService, times(1)).refreshAllRates();

            // When - second call succeeds
            scheduler.refreshExchangeRates();

            // Then - verify second call was made and succeeded
            verify(exchangeRateService, times(2)).refreshAllRates();
        }

        @Test
        @DisplayName("Should handle zero refreshed pairs")
        void shouldHandleZeroRefreshedPairs() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(0);

            // When
            scheduler.refreshExchangeRates();

            // Then
            verify(exchangeRateService).refreshAllRates();
            // Verify it completes without issues even with 0 pairs
        }

        @Test
        @DisplayName("Should handle service returning negative count gracefully")
        void shouldHandleNegativeCountGracefully() {
            // Given - simulate unexpected negative return
            when(exchangeRateService.refreshAllRates()).thenReturn(-1);

            // When & Then - should not throw exception
            assertThatCode(() -> scheduler.refreshExchangeRates())
                    .doesNotThrowAnyException();

            verify(exchangeRateService).refreshAllRates();
        }
    }

    @Nested
    @DisplayName("triggerManualRefresh Tests")
    class TriggerManualRefreshTests {

        @Test
        @DisplayName("Should successfully trigger manual refresh")
        void shouldSuccessfullyTriggerManualRefresh() {
            // Given
            int expectedCount = 8;
            when(exchangeRateService.refreshAllRates()).thenReturn(expectedCount);

            // When
            int result = scheduler.triggerManualRefresh();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(exchangeRateService).refreshAllRates();
        }

        @Test
        @DisplayName("Should return count of refreshed currency pairs")
        void shouldReturnCountOfRefreshedPairs() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(12);

            // When
            int result = scheduler.triggerManualRefresh();

            // Then
            assertThat(result).isEqualTo(12);
        }

        @Test
        @DisplayName("Should throw RuntimeException when refresh fails")
        void shouldThrowRuntimeExceptionWhenRefreshFails() {
            // Given
            RuntimeException cause = new RuntimeException("Database connection failed");
            when(exchangeRateService.refreshAllRates()).thenThrow(cause);

            // When & Then
            assertThatThrownBy(() -> scheduler.triggerManualRefresh())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Manual refresh failed")
                    .hasCause(cause);

            verify(exchangeRateService).refreshAllRates();
        }

        @Test
        @DisplayName("Should propagate exception with original message")
        void shouldPropagateExceptionWithOriginalMessage() {
            // Given
            String originalMessage = "All providers unavailable";
            RuntimeException originalException = new RuntimeException(originalMessage);
            when(exchangeRateService.refreshAllRates()).thenThrow(originalException);

            // When & Then
            assertThatThrownBy(() -> scheduler.triggerManualRefresh())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(originalMessage);
        }

        @Test
        @DisplayName("Should handle zero refreshed pairs in manual refresh")
        void shouldHandleZeroRefreshedPairsInManualRefresh() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(0);

            // When
            int result = scheduler.triggerManualRefresh();

            // Then
            assertThat(result).isZero();
            verify(exchangeRateService).refreshAllRates();
        }

        @Test
        @DisplayName("Should be callable multiple times")
        void shouldBeCallableMultipleTimes() {
            // Given
            when(exchangeRateService.refreshAllRates())
                    .thenReturn(5)
                    .thenReturn(7)
                    .thenReturn(6);

            // When
            int result1 = scheduler.triggerManualRefresh();
            int result2 = scheduler.triggerManualRefresh();
            int result3 = scheduler.triggerManualRefresh();

            // Then
            assertThat(result1).isEqualTo(5);
            assertThat(result2).isEqualTo(7);
            assertThat(result3).isEqualTo(6);
            verify(exchangeRateService, times(3)).refreshAllRates();
        }
    }

    @Nested
    @DisplayName("Integration Behavior Tests")
    class IntegrationBehaviorTests {

        @Test
        @DisplayName("Should call service exactly once per scheduled execution")
        void shouldCallServiceOncePerExecution() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(5);

            // When
            scheduler.refreshExchangeRates();

            // Then
            verify(exchangeRateService, times(1)).refreshAllRates();
            verifyNoMoreInteractions(exchangeRateService);
        }

        @Test
        @DisplayName("Should isolate manual refresh from scheduled refresh")
        void shouldIsolateManualFromScheduledRefresh() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(3);

            // When - trigger both manual and scheduled
            scheduler.triggerManualRefresh();
            scheduler.refreshExchangeRates();

            // Then - service should be called twice (once for each)
            verify(exchangeRateService, times(2)).refreshAllRates();
        }

        @Test
        @DisplayName("Should handle rapid consecutive calls")
        void shouldHandleRapidConsecutiveCalls() {
            // Given
            when(exchangeRateService.refreshAllRates()).thenReturn(4);

            // When - simulate rapid calls
            scheduler.refreshExchangeRates();
            scheduler.refreshExchangeRates();
            scheduler.refreshExchangeRates();

            // Then - each call should execute
            verify(exchangeRateService, times(3)).refreshAllRates();
        }
    }
}
