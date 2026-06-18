package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.BusinessException;
import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.CompanyOverviewDto;
import com.dhbw.webeng2.stomo.model.dto.SearchTickerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Alpha Vantage integration: company overview and symbol search.
 */
@Service
@Slf4j
public class StockService {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final String apiKey;

    public StockService(@Value("${api.url.alphavantage}") String baseUrl,
                        @Value("${api.key.alphavantage}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public CompanyOverviewDto getOverview(String symbol) {
        log.info("Fetching Alpha Vantage overview for {}", symbol);

        Map<String, Object> root = call(uriBuilder -> uriBuilder
                .queryParam("function", "OVERVIEW")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .build());

        // Unknown symbol -> empty JSON object.
        if (root.get("Symbol") == null) {
            throw new ResourceNotFoundException("No company overview found for symbol: " + symbol);
        }

        return CompanyOverviewDto.builder()
                .symbol(toStr(root.get("Symbol")))
                .name(toStr(root.get("Name")))
                .exchange(toStr(root.get("Exchange")))
                .sector(capitalize(toStr(root.get("Sector"))))
                .description(toStr(root.get("Description")))
                .marketCap(toLong(root.get("MarketCapitalization")))
                .peRatio(toDouble(root.get("PERatio")))
                .dividendYield(toDouble(root.get("DividendYield")))
                .week52High(toDouble(root.get("52WeekHigh")))
                .week52Low(toDouble(root.get("52WeekLow")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<SearchTickerDto> search(String keywords) {
        log.info("Searching Alpha Vantage tickers for '{}'", keywords);

        Map<String, Object> root = call(uriBuilder -> uriBuilder
                .queryParam("function", "SYMBOL_SEARCH")
                .queryParam("keywords", keywords)
                .queryParam("apikey", apiKey)
                .build());

        List<SearchTickerDto> result = new ArrayList<>();
        if (root.get("bestMatches") instanceof List<?> matches) {
            for (Object o : matches) {
                Map<String, Object> m = (Map<String, Object>) o;
                result.add(SearchTickerDto.builder()
                        .symbol(toStr(m.get("1. symbol")))
                        .name(toStr(m.get("2. name")))
                        .type(toStr(m.get("3. type")))
                        .region(toStr(m.get("4. region")))
                        .build());
            }
        }
        return result;
    }

    private Map<String, Object> call(Function<UriBuilder, URI> uriFn) {
        Map<String, Object> root = webClient.get()
                .uri(uriFn)
                .retrieve()
                .bodyToMono(JSON_MAP)
                .block();

        if (root == null) {
            throw new BusinessException("Empty response from Alpha Vantage");
        }
        // Rate limit / informational responses come back with HTTP 200 and these keys.
        if (root.get("Note") != null || root.get("Information") != null) {
            Object msg = root.get("Note") != null ? root.get("Note") : root.get("Information");
            log.warn("Alpha Vantage limit/info: {}", msg);
            throw new BusinessException("Alpha Vantage rate limit reached or invalid request: " + msg);
        }
        if (root.get("Error Message") != null) {
            throw new ResourceNotFoundException(toStr(root.get("Error Message")));
        }
        return root;
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        String text = toStr(value);
        if (text == null || text.isBlank() || "None".equalsIgnoreCase(text) || "-".equals(text)) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        String text = toStr(value);
        if (text == null || text.isBlank() || "None".equalsIgnoreCase(text) || "-".equals(text)) return null;
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toStr(Object value) {
        return value == null ? null : value.toString();
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
