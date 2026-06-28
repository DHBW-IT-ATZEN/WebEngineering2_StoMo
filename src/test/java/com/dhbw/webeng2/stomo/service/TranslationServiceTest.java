package com.dhbw.webeng2.stomo.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the graceful-degradation behaviour of {@link TranslationService} when no
 * LibreTranslate instance is configured (the default). No network is involved.
 */
class TranslationServiceTest {

    private final TranslationService service = new TranslationService("", "");

    @Test
    void isDisabledWithoutAConfiguredUrl() {
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void passesEveryStringThroughUnchangedWhenDisabled() {
        List<String> texts = List.of("Hello", "Corporate Dossier");

        Map<String, String> result = service.translateBatch(texts, "de");

        assertThat(result).containsEntry("Hello", "Hello");
        assertThat(result).containsEntry("Corporate Dossier", "Corporate Dossier");
    }

    @Test
    void returnsEmptyMapForNullOrEmptyInput() {
        assertThat(service.translateBatch(null, "de")).isEmpty();
        assertThat(service.translateBatch(List.of(), "de")).isEmpty();
    }

    @Test
    void skipsBlankEntriesButKeepsRealOnes() {
        Map<String, String> result = service.translateBatch(Arrays.asList("Real", "  ", null), "de");

        assertThat(result).containsEntry("Real", "Real");
        assertThat(result).doesNotContainKey(null);
    }
}
