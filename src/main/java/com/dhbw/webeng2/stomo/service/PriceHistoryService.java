package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.QuoteDto;
import com.dhbw.webeng2.stomo.model.entity.PriceHistory;
import com.dhbw.webeng2.stomo.repository.PriceHistoryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the intraday 30-minute series per symbol in the database (source: Yahoo).
 * The client derives intraday/weekly/monthly views from this single series, so
 * one fetch backs every timeframe and survives reloads, restarts and other users.
 *
 * Concurrency: a per-symbol lock makes concurrent requests for the same symbol
 * coalesce into a single fetch + single write (the symbol is the primary key, so a
 * security can never be stored twice). If the upstream fails but cached data exists,
 * the stale data is served instead of erroring.
 *
 * Note: the lock is JVM-local, which is correct for a single backend instance.
 * A multi-instance deployment would need a DB-level lock / upsert instead.
 */
@Service
@Slf4j
public class PriceHistoryService {

    private final PriceHistoryRepo repo;
    private final YahooService yahoo;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public PriceHistoryService(PriceHistoryRepo repo,
                               YahooService yahoo,
                               ObjectMapper objectMapper,
                               @Value("${cache.history.ttl-minutes:720}") long ttlMinutes) {
        this.repo = repo;
        this.yahoo = yahoo;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Both cached granularities (30-min coarse + 10-min fine). The client resamples per timeframe. */
    public PriceSeriesDto getSeries(String symbol) {
        String key = symbol.toUpperCase();

        Optional<PriceHistory> cached = repo.findById(key);
        if (isFresh(cached)) {
            return build(key, cached.get());
        }

        synchronized (lockFor(key)) {
            // Re-check: another thread may have refreshed while we waited.
            cached = repo.findById(key);
            if (isFresh(cached)) {
                return build(key, cached.get());
            }

            try {
                List<QuoteDto> coarse = yahoo.fetch30m(key);
                List<QuoteDto> fine = yahoo.fetch10m(key);
                PriceHistory entity = cached.orElseGet(PriceHistory::new);
                entity.setSymbol(key);
                entity.setBarsJson(objectMapper.writeValueAsString(coarse));
                entity.setFineBarsJson(objectMapper.writeValueAsString(fine));
                entity.setFetchedAt(Instant.now());
                repo.save(entity);
                return PriceSeriesDto.builder().symbol(key).coarse(coarse).fine(fine).build();
            } catch (RuntimeException ex) {
                if (cached.isPresent() && cached.get().getBarsJson() != null) {
                    log.warn("Yahoo refresh failed for {} ({}); serving stale cache.", key, ex.getMessage());
                    return build(key, cached.get());
                }
                throw ex;
            }
        }
    }

    private PriceSeriesDto build(String symbol, PriceHistory entity) {
        return PriceSeriesDto.builder()
                .symbol(symbol)
                .coarse(deserialize(entity.getBarsJson()))
                .fine(deserialize(entity.getFineBarsJson()))
                .build();
    }

    private boolean isFresh(Optional<PriceHistory> cached) {
        return cached.isPresent()
                && cached.get().getBarsJson() != null
                && cached.get().getFineBarsJson() != null
                && cached.get().getFetchedAt() != null
                && cached.get().getFetchedAt().isAfter(Instant.now().minus(ttl));
    }

    private List<QuoteDto> deserialize(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        QuoteDto[] bars = objectMapper.readValue(json, QuoteDto[].class);
        return new ArrayList<>(Arrays.asList(bars));
    }

    /** Latest known price for a symbol: last 10-min close, falling back to the last 30-min close. */
    public Double getLatestPrice(String symbol) {
        PriceSeriesDto series = getSeries(symbol);
        Double fine = lastClose(series.getFine());
        return fine != null ? fine : lastClose(series.getCoarse());
    }

    private Double lastClose(List<QuoteDto> bars) {
        if (bars == null || bars.isEmpty()) return null;
        return bars.get(bars.size() - 1).getClose();
    }

    private Object lockFor(String key) {
        return locks.computeIfAbsent(key, k -> new Object());
    }
}
