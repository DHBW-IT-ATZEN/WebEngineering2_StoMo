package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.GlobalQuoteDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Finnhub integration: real-time price quotes.
 */
@Service
@Slf4j
public class ReportService {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final String apiKey;

    public ReportService(@Value("${api.url.finnhub}") String baseUrl,
                         @Value("${api.key.finnhub}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public GlobalQuoteDto getQuote(String symbol) {
        log.info("Fetching Finnhub quote for {}", symbol);

        Map<String, Object> body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JSON_MAP)
                .block();

        Double current = toDouble(body == null ? null : body.get("c"));
        // Finnhub returns c=0 for an unknown symbol instead of an error.
        if (current == null || current == 0d) {
            throw new ResourceNotFoundException("No quote found for symbol: " + symbol);
        }

        Long timestamp = toLong(body.get("t"));
        String tradingDay = (timestamp != null && timestamp > 0)
                ? Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                : null;

        return GlobalQuoteDto.builder()
                .symbol(symbol.toUpperCase())
                .price(current)
                .change(toDouble(body.get("d")))
                .changePercent(toDouble(body.get("dp")))
                .open(toDouble(body.get("o")))
                .high(toDouble(body.get("h")))
                .low(toDouble(body.get("l")))
                .previousClose(toDouble(body.get("pc")))
                .latestTradingDay(tradingDay)
                .build();
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
