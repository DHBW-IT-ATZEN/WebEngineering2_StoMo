package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.model.entity.YodaTranslation;
import com.dhbw.webeng2.stomo.repository.YodaTranslationRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Yoda (yodish) translation via RapidAPI.
 *
 * Cost control: every translation is cached in the database, so each distinct
 * source string hits the paid API at most once. When no API key is configured
 * (or a call fails) the original text is returned unchanged, so the feature
 * degrades gracefully instead of breaking the UI.
 */
@Service
@Slf4j
public class YodaService {

    private final WebClient webClient;
    private final String apiKey;
    private final String apiHost;
    private final YodaTranslationRepo repo;
    private final ObjectMapper objectMapper;

    public YodaService(@Value("${api.url.yoda:https://yodish.p.rapidapi.com}") String baseUrl,
                       @Value("${api.host.yoda:yodish.p.rapidapi.com}") String apiHost,
                       @Value("${api.key.yoda:}") String apiKey,
                       YodaTranslationRepo repo,
                       ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String translateOne(String text) {
        if (text == null || text.isBlank()) return text;
        return repo.findBySource(text)
                .map(YodaTranslation::getTranslated)
                .orElseGet(() -> fetchAndCache(text));
    }

    public Map<String, String> translateBatch(List<String> texts) {
        Map<String, String> result = new LinkedHashMap<>();
        if (texts == null || texts.isEmpty()) return result;

        List<String> distinct = texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        Map<String, String> known = repo.findBySourceIn(distinct).stream()
                .collect(Collectors.toMap(YodaTranslation::getSource, YodaTranslation::getTranslated, (a, b) -> a));

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                if (text != null) result.put(text, text);
                continue;
            }
            String translated = known.computeIfAbsent(text, this::fetchAndCache);
            result.put(text, translated);
        }
        return result;
    }

    private String fetchAndCache(String text) {
        if (!isEnabled()) {
            return text; // no key -> passthrough, not cached so a future key still translates
        }
        try {
            String raw = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/yodish").queryParam("text", text).build())
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", apiHost)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String translated = parseTranslation(raw, text);

            YodaTranslation entity = new YodaTranslation();
            entity.setSource(text);
            entity.setTranslated(translated);
            repo.save(entity);
            return translated;
        } catch (Exception e) {
            log.warn("Yoda translation failed for '{}': {}", text, e.getMessage());
            return text; // graceful fallback, not cached
        }
    }

    /** The yodish API may answer with plain text or a JSON envelope; handle both. */
    private String parseTranslation(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String trimmed = raw.trim();
        if (trimmed.charAt(0) != '{' && trimmed.charAt(0) != '[') {
            return trimmed;
        }
        try {
            Map<?, ?> json = objectMapper.readValue(trimmed, Map.class);
            Object contents = json.get("contents");
            if (contents instanceof Map<?, ?> contentMap) {
                Object nested = contentMap.get("translated");
                if (nested != null) return nested.toString();
            }
            for (String key : List.of("translated", "yodish", "text", "translation")) {
                Object value = json.get(key);
                if (value != null) return value.toString();
            }
        } catch (Exception e) {
            log.debug("Could not parse Yoda response as JSON: {}", e.getMessage());
            return trimmed;
        }
        return fallback;
    }
}
