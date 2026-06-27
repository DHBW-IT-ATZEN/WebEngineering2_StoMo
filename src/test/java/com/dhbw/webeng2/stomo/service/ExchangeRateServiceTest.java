package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.model.entity.ExchangeRate;
import com.dhbw.webeng2.stomo.repository.ExchangeRateRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the start-of-day FX cache: USD pinned, per-day fetch & cache, stale-on-failure.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepo repo;
    @Mock
    private YahooService yahoo;

    private ExchangeRateService service(String currencies) {
        return new ExchangeRateService(repo, yahoo, currencies, "USD,EUR");
    }

    private static TickerDto fx(double usdPerUnit) {
        return TickerDto.builder().symbol("X").price(usdPerUnit).changePct(0.0).currency("USD").build();
    }

    private static ExchangeRate stored(String ccy, double rate, LocalDate asOf) {
        ExchangeRate e = new ExchangeRate();
        e.setCurrency(ccy);
        e.setRateToUsd(rate);
        e.setAsOfDate(asOf);
        e.setFetchedAt(Instant.now());
        return e;
    }

    @Test
    void pinsUsdAndFetchesMissingRates() {
        when(repo.findById("EUR")).thenReturn(Optional.empty());
        when(repo.findById("JPY")).thenReturn(Optional.empty());
        when(yahoo.fetchDailyQuote("EURUSD=X")).thenReturn(fx(1.1393));
        when(yahoo.fetchDailyQuote("JPYUSD=X")).thenReturn(fx(0.0062));

        Map<String, Double> rates = service("USD,EUR,JPY").getRatesToUsd();

        assertThat(rates)
                .containsEntry("USD", 1.0)
                .containsEntry("EUR", 1.1393)
                .containsEntry("JPY", 0.0062);
        verify(repo, times(2)).save(any());
    }

    @Test
    void servesTodaysCachedRateWithoutFetching() {
        when(repo.findById("EUR")).thenReturn(Optional.of(stored("EUR", 1.2, LocalDate.now())));

        Map<String, Double> rates = service("USD,EUR").getRatesToUsd();

        assertThat(rates).containsEntry("EUR", 1.2);
        verify(yahoo, never()).fetchDailyQuote("EURUSD=X");
        verify(repo, never()).save(any());
    }

    @Test
    void servesStaleRateWhenUpstreamFails() {
        when(repo.findById("EUR")).thenReturn(Optional.of(stored("EUR", 1.05, LocalDate.now().minusDays(1))));
        when(yahoo.fetchDailyQuote("EURUSD=X")).thenThrow(new RuntimeException("upstream down"));

        Map<String, Double> rates = service("USD,EUR").getRatesToUsd();

        assertThat(rates).containsEntry("EUR", 1.05); // yesterday's rate, not dropped
    }

    @Test
    void exposesConfiguredDisplayOptions() {
        assertThat(service("USD,EUR,JPY").getDisplayOptions()).containsExactly("USD", "EUR");
    }
}
