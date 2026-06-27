package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.dto.TickerDto;
import com.dhbw.webeng2.stomo.model.entity.ExchangeRate;
import com.dhbw.webeng2.stomo.repository.ExchangeRateRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Start-of-day FX rates to USD for the configured currencies. Each rate is fetched from Yahoo's
 * {@code {CCY}USD=X} pair (USD per 1 unit) the first time it's needed on a given day, then cached
 * in the database and held for the rest of the day — a start-of-day snapshot. USD is pinned to
 * 1.0. If an upstream fetch fails, the last stored rate is served instead of erroring, so price
 * conversion degrades gracefully (mirrors {@link PriceHistoryService}/{@link BannerService}).
 */
@Service
@Slf4j
public class ExchangeRateService {

    private static final String BASE = "USD";

    private final ExchangeRateRepo repo;
    private final YahooService yahoo;
    private final List<String> currencies;
    private final List<String> displayOptions;

    public ExchangeRateService(ExchangeRateRepo repo,
                               YahooService yahoo,
                               @Value("${app.fx.currencies:}") String currenciesCsv,
                               @Value("${app.fx.display-options:}") String displayCsv) {
        this.repo = repo;
        this.yahoo = yahoo;
        this.currencies = parseCsv(currenciesCsv);
        this.displayOptions = parseCsv(displayCsv);
    }

    /** Map of currency code -> USD per 1 unit, for every configured currency (USD = 1.0). */
    public Map<String, Double> getRatesToUsd() {
        LocalDate today = LocalDate.now();
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put(BASE, 1.0);
        for (String ccy : currencies) {
            if (BASE.equals(ccy)) continue;
            Double rate = ensureRate(ccy, today);
            if (rate != null) rates.put(ccy, rate);
        }
        return rates;
    }

    /** Currencies offered in the UI display-currency picker. */
    public List<String> getDisplayOptions() {
        return displayOptions;
    }

    public String getBase() {
        return BASE;
    }

    /** Today's stored rate for the currency, fetching & caching it once per day; stale on failure. */
    private Double ensureRate(String ccy, LocalDate today) {
        Optional<ExchangeRate> cached = repo.findById(ccy);
        if (cached.isPresent() && today.equals(cached.get().getAsOfDate())) {
            return cached.get().getRateToUsd();
        }
        try {
            TickerDto fx = yahoo.fetchDailyQuote(ccy + BASE + "=X"); // e.g. JPYUSD=X -> USD per 1 JPY
            if (fx != null && fx.getPrice() != null && fx.getPrice() > 0) {
                ExchangeRate entity = cached.orElseGet(ExchangeRate::new);
                entity.setCurrency(ccy);
                entity.setRateToUsd(fx.getPrice());
                entity.setAsOfDate(today);
                entity.setFetchedAt(Instant.now());
                repo.save(entity);
                return fx.getPrice();
            }
        } catch (RuntimeException ex) {
            log.warn("FX refresh failed for {} ({}); serving last known rate.", ccy, ex.getMessage());
        }
        return cached.map(ExchangeRate::getRateToUsd).orElse(null);
    }

    private static List<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
