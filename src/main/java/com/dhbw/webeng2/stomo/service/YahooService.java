package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.QuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delayed intraday history via Yahoo's public chart endpoint (keyless).
 * One call returns ~60 days of 30-minute bars; weekly/monthly views are derived
 * from this on the client. Note: this is an unofficial endpoint, hence the
 * defensive parsing and graceful errors.
 */
@Service
@Slf4j
public class YahooService {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {};
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WebClient webClient;

    public YahooService(@Value("${api.url.yahoo:https://query1.finance.yahoo.com}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    /** 30-minute bars over ~60 days — backs the weekly & monthly views. */
    public List<QuoteDto> fetch30m(String symbol) {
        return fetchBars(symbol, "30m", "60d");
    }

    /** 10-minute bars (aggregated from Yahoo's 5m) over a few days — backs the intraday view. */
    public List<QuoteDto> fetch10m(String symbol) {
        return aggregateTo10m(fetchBars(symbol, "5m", "5d"));
    }

    @SuppressWarnings("unchecked")
    private List<QuoteDto> fetchBars(String symbol, String interval, String range) {
        log.info("Fetching Yahoo {} bars ({}) for {}", interval, range, symbol);

        Map<String, Object> root = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v8/finance/chart/{symbol}")
                        .queryParam("interval", interval)
                        .queryParam("range", range)
                        .build(symbol))
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .bodyToMono(JSON_MAP)
                .block();

        Map<String, Object> chart = root == null ? null : (Map<String, Object>) root.get("chart");
        if (chart == null) {
            throw new ResourceNotFoundException("No chart data for symbol: " + symbol);
        }
        if (chart.get("error") != null) {
            throw new ResourceNotFoundException("No data for symbol: " + symbol);
        }
        List<Object> results = (List<Object>) chart.get("result");
        if (results == null || results.isEmpty()) {
            throw new ResourceNotFoundException("No data for symbol: " + symbol);
        }

        Map<String, Object> result0 = (Map<String, Object>) results.get(0);
        Map<String, Object> meta = (Map<String, Object>) result0.get("meta");
        long gmtOffset = meta == null ? 0L : toLong(meta.get("gmtoffset"));

        List<Object> timestamps = (List<Object>) result0.get("timestamp");
        Map<String, Object> indicators = (Map<String, Object>) result0.get("indicators");
        List<Object> quoteList = indicators == null ? null : (List<Object>) indicators.get("quote");
        if (timestamps == null || quoteList == null || quoteList.isEmpty()) {
            throw new ResourceNotFoundException("No intraday history for symbol: " + symbol);
        }
        Map<String, Object> quote0 = (Map<String, Object>) quoteList.get(0);
        List<Object> opens = (List<Object>) quote0.get("open");
        List<Object> highs = (List<Object>) quote0.get("high");
        List<Object> lows = (List<Object>) quote0.get("low");
        List<Object> closes = (List<Object>) quote0.get("close");
        List<Object> volumes = (List<Object>) quote0.get("volume");

        List<QuoteDto> bars = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            Double close = toDouble(at(closes, i));
            if (close == null) continue; // gap / non-trading slot
            long localEpoch = toLong(timestamps.get(i)) + gmtOffset;
            String stamp = Instant.ofEpochSecond(localEpoch).atZone(ZoneOffset.UTC).format(STAMP);
            bars.add(QuoteDto.builder()
                    .date(stamp)
                    .open(toDouble(at(opens, i)))
                    .high(toDouble(at(highs, i)))
                    .low(toDouble(at(lows, i)))
                    .close(close)
                    .volume(toLongOrNull(at(volumes, i)))
                    .build());
        }
        if (bars.isEmpty()) {
            throw new ResourceNotFoundException("No intraday history for symbol: " + symbol);
        }
        return bars;
    }

    /** Combine 5-minute bars into 10-minute candles (Yahoo has no native 10m interval). */
    private static List<QuoteDto> aggregateTo10m(List<QuoteDto> fiveMin) {
        LinkedHashMap<String, List<QuoteDto>> buckets = new LinkedHashMap<>();
        for (QuoteDto bar : fiveMin) {
            String date = bar.getDate(); // "yyyy-MM-dd HH:mm"
            int minute = Integer.parseInt(date.substring(14, 16));
            String key = date.substring(0, 14) + String.format("%02d", (minute / 10) * 10);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(bar);
        }

        List<QuoteDto> out = new ArrayList<>();
        for (Map.Entry<String, List<QuoteDto>> entry : buckets.entrySet()) {
            List<QuoteDto> group = entry.getValue();
            QuoteDto first = group.get(0);
            QuoteDto last = group.get(group.size() - 1);
            Double high = null;
            Double low = null;
            long volume = 0;
            boolean hasVolume = false;
            for (QuoteDto bar : group) {
                if (bar.getHigh() != null) high = (high == null) ? bar.getHigh() : Math.max(high, bar.getHigh());
                if (bar.getLow() != null) low = (low == null) ? bar.getLow() : Math.min(low, bar.getLow());
                if (bar.getVolume() != null) {
                    volume += bar.getVolume();
                    hasVolume = true;
                }
            }
            out.add(QuoteDto.builder()
                    .date(entry.getKey())
                    .open(first.getOpen())
                    .high(high)
                    .low(low)
                    .close(last.getClose())
                    .volume(hasVolume ? volume : null)
                    .build());
        }
        return out;
    }

    private static Object at(List<Object> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    private static Double toDouble(Object value) {
        return (value instanceof Number n) ? n.doubleValue() : null;
    }

    private static long toLong(Object value) {
        return (value instanceof Number n) ? n.longValue() : 0L;
    }

    private static Long toLongOrNull(Object value) {
        return (value instanceof Number n) ? n.longValue() : null;
    }
}
