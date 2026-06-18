package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.repository.WatchlistItemRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the homepage ticker banner. By default it shows a curated global set (the five major
 * indices plus large caps). Once users have collectively watched more than
 * {@code app.banner.dynamic-threshold} distinct symbols, it switches to the 20 most-watched
 * symbols across all watchlists — a live reflection of what users are tracking. Prices and day
 * changes come from Yahoo; the assembled banner is cached in memory for a few minutes so the
 * landing page never triggers a burst of upstream calls.
 */
@Service
@Slf4j
public class BannerService {

    private static final List<String> DEFAULT_SYMBOLS = List.of(
            "^GSPC", "^IXIC", "^DJI", "^GDAXI", "^N225",   // S&P 500, Nasdaq, Dow, DAX, Nikkei
            "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "SAP", "7203.T");

    private final WatchlistItemRepo watchlistItemRepo;
    private final YahooService yahoo;
    private final long dynamicThreshold;
    private final Duration ttl;

    private volatile List<TickerDto> cached;
    private volatile Instant cachedAt;

    public BannerService(WatchlistItemRepo watchlistItemRepo,
                         YahooService yahoo,
                         @Value("${app.banner.dynamic-threshold:20}") long dynamicThreshold,
                         @Value("${app.banner.ttl-minutes:5}") long ttlMinutes) {
        this.watchlistItemRepo = watchlistItemRepo;
        this.yahoo = yahoo;
        this.dynamicThreshold = dynamicThreshold;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public List<TickerDto> getBanner() {
        List<TickerDto> snapshot = cached;
        if (snapshot != null && cachedAt != null && cachedAt.isAfter(Instant.now().minus(ttl))) {
            return snapshot;
        }

        List<TickerDto> items = new ArrayList<>();
        for (String symbol : chooseSymbols()) {
            try {
                TickerDto quote = yahoo.fetchDailyQuote(symbol);
                if (quote != null) {
                    items.add(quote);
                }
            } catch (RuntimeException ex) {
                log.debug("Banner: skipping {} ({})", symbol, ex.getMessage());
            }
        }

        if (items.isEmpty()) {
            return snapshot != null ? snapshot : items; // serve stale rather than an empty banner
        }
        cached = items;
        cachedAt = Instant.now();
        return items;
    }

    /** Most-watched symbols once enough have accumulated; otherwise the curated default set. */
    private List<String> chooseSymbols() {
        if (watchlistItemRepo.countDistinctSymbols() > dynamicThreshold) {
            List<String> topWatched = watchlistItemRepo.findTopSymbols(PageRequest.of(0, 20));
            if (!topWatched.isEmpty()) {
                return topWatched;
            }
        }
        return DEFAULT_SYMBOLS;
    }
}
