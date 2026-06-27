package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.PriceSeriesDto;
import com.dhbw.webeng2.stomo.model.dto.QuoteDto;
import com.dhbw.webeng2.stomo.model.entity.PriceHistory;
import com.dhbw.webeng2.stomo.repository.PriceHistoryRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Caches the intraday price series per symbol in the database (source: Yahoo). The client
 * derives intraday/weekly/monthly views from this single series, so one fetch backs every
 * timeframe and survives reloads, restarts and other users.
 *
 * Concurrency: the symbol is the primary key and the row carries an optimistic-locking
 * {@code @Version}, so concurrent refreshes are coordinated by the database — across threads
 * <em>and</em> across multiple backend instances. The first writer wins; a loser (an
 * optimistic-lock or duplicate-key failure) just reads the row back and serves what the winner
 * wrote. If the upstream fails but cached data exists, the stale data is served instead of
 * erroring. (This replaces the previous JVM-local lock, which only coordinated one instance.)
 */
@Service
@Slf4j
public class PriceHistoryService {

    private final PriceHistoryRepo repo;
    private final YahooService yahoo;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

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

        try {
            YahooService.BarSeries coarse = yahoo.fetch30m(key);
            YahooService.BarSeries fine = yahoo.fetch10m(key);
            String currency = coarse.currency() != null ? coarse.currency() : fine.currency();
            String type = coarse.type() != null ? coarse.type() : fine.type();
            if (type == null) type = "UNKNOWN"; // keep non-null so a typeless symbol isn't re-fetched forever
            PriceHistory entity = cached.orElseGet(PriceHistory::new);
            entity.setSymbol(key);
            entity.setBarsJson(objectMapper.writeValueAsString(coarse.bars()));
            entity.setFineBarsJson(objectMapper.writeValueAsString(fine.bars()));
            entity.setCurrency(currency);
            entity.setType(type);
            entity.setFetchedAt(Instant.now());
            repo.save(entity);
            return PriceSeriesDto.builder()
                    .symbol(key).currency(currency).type(type)
                    .coarse(coarse.bars()).fine(fine.bars()).build();
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            // Another thread/instance refreshed this symbol concurrently — the DB row is the
            // single source of truth, so serve what the winner just wrote.
            return repo.findById(key)
                    .map(e -> build(key, e))
                    .orElseThrow(() -> ex);
        } catch (RuntimeException ex) {
            if (cached.isPresent() && cached.get().getBarsJson() != null) {
                log.warn("Yahoo refresh failed for {} ({}); serving stale cache.", key, ex.getMessage());
                return build(key, cached.get());
            }
            throw ex;
        }
    }

    private PriceSeriesDto build(String symbol, PriceHistory entity) {
        return PriceSeriesDto.builder()
                .symbol(symbol)
                .currency(entity.getCurrency())
                .type(entity.getType())
                .coarse(deserialize(entity.getBarsJson()))
                .fine(deserialize(entity.getFineBarsJson()))
                .build();
    }

    private boolean isFresh(Optional<PriceHistory> cached) {
        return cached.isPresent()
                && cached.get().getBarsJson() != null
                && cached.get().getFineBarsJson() != null
                // Force a refresh of legacy rows cached before the currency/type columns existed,
                // so they pick up the instrument type (an index then renders as points, not money).
                && cached.get().getType() != null
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

    /** Native listing currency for a symbol, from the cached series (nullable). */
    public String getCurrency(String symbol) {
        return getSeries(symbol).getCurrency();
    }

    /** Instrument type (EQUITY, INDEX, …) for a symbol, from the cached series (nullable). */
    public String getType(String symbol) {
        return getSeries(symbol).getType();
    }

    private Double lastClose(List<QuoteDto> bars) {
        if (bars == null || bars.isEmpty()) return null;
        return bars.get(bars.size() - 1).getClose();
    }
}
