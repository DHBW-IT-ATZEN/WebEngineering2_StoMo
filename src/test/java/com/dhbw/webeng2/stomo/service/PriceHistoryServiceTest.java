package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.QuoteDto;
import com.dhbw.webeng2.stomo.model.entity.PriceHistory;
import com.dhbw.webeng2.stomo.repository.PriceHistoryRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the price-history cache freshness, in particular the self-heal of legacy rows
 * cached before the instrument-type column existed (so an index re-fetches and becomes points).
 */
@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepo repo;
    @Mock
    private YahooService yahoo;
    @Mock
    private ObjectMapper objectMapper;

    private PriceHistoryService service() {
        return new PriceHistoryService(repo, yahoo, objectMapper, 720);
    }

    private static PriceHistory row(String type) {
        PriceHistory e = new PriceHistory();
        e.setSymbol("^DJI");
        e.setBarsJson("[]");
        e.setFineBarsJson("[]");
        e.setCurrency("USD");
        e.setType(type);
        e.setFetchedAt(Instant.now()); // within TTL
        return e;
    }

    private static QuoteDto quote(double close) {
        return QuoteDto.builder().date("2026-06-23 15:50").open(close).high(close).low(close).close(close).volume(1L).build();
    }

    @Test
    void refetchesLegacyRowMissingType() {
        when(repo.findById("^DJI")).thenReturn(Optional.of(row(null))); // cached before the type column
        when(yahoo.fetch30m("^DJI")).thenReturn(new YahooService.BarSeries(List.of(quote(51849)), "USD", "INDEX"));
        when(yahoo.fetch10m("^DJI")).thenReturn(new YahooService.BarSeries(List.of(quote(51849)), "USD", "INDEX"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        PriceSeriesDto out = service().getSeries("^DJI");

        assertThat(out.getType()).isEqualTo("INDEX"); // healed -> now an index -> renders as points
        verify(yahoo).fetch30m("^DJI");               // i.e. it did NOT serve the stale row
    }

    @Test
    void refetchesCorruptCachedRowInsteadOfFailing() {
        PriceHistory corrupt = row("EQUITY");
        corrupt.setBarsJson("32836");     // legacy Large-Object OID stored as text, not JSON
        corrupt.setFineBarsJson("32837");
        when(repo.findById("^DJI")).thenReturn(Optional.of(corrupt));
        when(objectMapper.readValue("32836", QuoteDto[].class))
                .thenThrow(new RuntimeException("Cannot deserialize QuoteDto[] from Integer value"));
        when(yahoo.fetch30m("^DJI")).thenReturn(new YahooService.BarSeries(List.of(quote(51849)), "USD", "INDEX"));
        when(yahoo.fetch10m("^DJI")).thenReturn(new YahooService.BarSeries(List.of(quote(51849)), "USD", "INDEX"));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        PriceSeriesDto out = service().getSeries("^DJI");

        assertThat(out.getCoarse()).hasSize(1);   // served fresh data, not the corrupt row
        verify(yahoo).fetch30m("^DJI");            // i.e. it refetched rather than throwing
    }

    @Test
    void servesFreshRowThatAlreadyHasType() {
        when(repo.findById("^DJI")).thenReturn(Optional.of(row("INDEX")));
        when(objectMapper.readValue("[]", QuoteDto[].class)).thenReturn(new QuoteDto[0]);

        PriceSeriesDto out = service().getSeries("^DJI");

        assertThat(out.getType()).isEqualTo("INDEX");
        verifyNoInteractions(yahoo); // served from cache, no upstream call
    }
}
