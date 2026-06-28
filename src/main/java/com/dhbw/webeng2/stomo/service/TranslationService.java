package com.dhbw.webeng2.stomo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic machine translation of arbitrary text (e.g. company descriptions) via a self-hosted
 * LibreTranslate instance. Static UI labels are handled by the frontend dictionary (de.js); this
 * covers the strings that can't be pre-translated.
 *
 * The whole feature is opt-in: with no {@code api.url.libretranslate} configured it stays disabled
 * and every string passes through unchanged, so the app works without the translation container.
 */
@Service
@Slf4j
public class TranslationService {

    private final WebClient webClient;
    private final String apiKey;
    private final boolean enabled;

    public TranslationService(@Value("${api.url.libretranslate:}") String baseUrl,
                              @Value("${api.key.libretranslate:}") String apiKey) {
        this.enabled = baseUrl != null && !baseUrl.isBlank();
        this.webClient = enabled ? WebClient.builder().baseUrl(baseUrl).build() : null;
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Translate every source string to {@code target} (e.g. "de"). Distinct, non-blank strings go
     * to LibreTranslate in a single batch; anything that can't be translated (disabled or upstream
     * down) falls back to the original text so the UI never breaks.
     */
    public Map<String, String> translateBatch(List<String> texts, String target) {
        Map<String, String> result = new LinkedHashMap<>();
        if (texts == null || texts.isEmpty()) return result;

        List<String> distinct = texts.stream()
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        Map<String, String> translated = (enabled && !distinct.isEmpty())
                ? fetch(distinct, target)
                : Map.of();

        for (String text : texts) {
            if (text == null) continue;
            result.put(text, translated.getOrDefault(text, text));
        }
        return result;
    }

    private Map<String, String> fetch(List<String> texts, String target) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("q", texts);          // LibreTranslate accepts an array and answers with an array
            body.put("source", "en");
            body.put("target", target);
            body.put("format", "text");
            if (apiKey != null && !apiKey.isBlank()) body.put("api_key", apiKey);

            LibreTranslateResponse resp = webClient.post()
                    .uri("/translate")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(LibreTranslateResponse.class)
                    .block();

            Map<String, String> map = new LinkedHashMap<>();
            if (resp != null && resp.translatedText() != null) {
                List<String> out = resp.translatedText();
                for (int i = 0; i < texts.size() && i < out.size(); i++) {
                    map.put(texts.get(i), out.get(i));
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("LibreTranslate request for target '{}' failed: {}", target, e.getMessage());
            return Map.of();
        }
    }

    /** {@code { "translatedText": [...] }} — LibreTranslate returns an array when {@code q} is one. */
    record LibreTranslateResponse(List<String> translatedText) {
    }
}
