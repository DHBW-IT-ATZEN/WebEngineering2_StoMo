package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.repository.WatchlistItemRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the banner source-selection (curated default vs most-watched) and caching.
 */
@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private WatchlistItemRepo watchlistItemRepo;
    @Mock
    private YahooService yahoo;
    private BannerService service;

    @BeforeEach
    void setUp() {
        service = new BannerService(watchlistItemRepo, yahoo, 20, 5, "^GSPC,^DJI,AAPL,MSFT");
        lenient().when(yahoo.fetchDailyQuote(anyString())).thenAnswer(inv ->
                TickerDto.builder().symbol(inv.getArgument(0)).price(1.0).changePct(0.0).build());
    }

    @Test
    void usesCuratedDefaultsBelowThreshold() {
        when(watchlistItemRepo.countDistinctSymbols()).thenReturn(5L);

        List<TickerDto> banner = service.getBanner();

        assertThat(banner).extracting(TickerDto::getSymbol).contains("AAPL", "^GSPC");
        verify(watchlistItemRepo, never()).findTopSymbols(any());
    }

    @Test
    void usesMostWatchedAboveThreshold() {
        when(watchlistItemRepo.countDistinctSymbols()).thenReturn(25L);
        when(watchlistItemRepo.findTopSymbols(any())).thenReturn(List.of("AAA", "BBB"));

        List<TickerDto> banner = service.getBanner();

        assertThat(banner).extracting(TickerDto::getSymbol).containsExactly("AAA", "BBB");
    }

    @Test
    void cachesBetweenCalls() {
        when(watchlistItemRepo.countDistinctSymbols()).thenReturn(5L);

        service.getBanner();
        service.getBanner();

        verify(watchlistItemRepo, times(1)).countDistinctSymbols(); // second call served from cache
    }

    /**
     * Boots a minimal Spring context to prove the comma-separated app.banner.default-symbols
     * property binds to a List<String> (split into distinct symbols, order preserved) rather
     * than a single combined string — the behaviour the curated set now relies on.
     */
    @Test
    void bindsCommaSeparatedDefaultSymbolsFromConfig() {
        when(watchlistItemRepo.countDistinctSymbols()).thenReturn(0L); // below threshold -> curated defaults

        new ApplicationContextRunner()
                .withBean(WatchlistItemRepo.class, () -> watchlistItemRepo)
                .withBean(YahooService.class, () -> yahoo)
                .withBean(BannerService.class)
                .withPropertyValues(
                        "app.banner.default-symbols=^GSPC,^DJI,AAPL",
                        "app.banner.dynamic-threshold=20",
                        "app.banner.ttl-minutes=5")
                .run(context -> assertThat(context.getBean(BannerService.class).getBanner())
                        .extracting(TickerDto::getSymbol)
                        .containsExactly("^GSPC", "^DJI", "AAPL"));
    }
}
