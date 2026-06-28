package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.model.dto.TranslateRequest;
import com.dhbw.webeng2.stomo.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    /** Batch translate: { "texts": [...], "target": "de" } -> { "<source>": "<translated>", ... }. */
    @PostMapping
    public ResponseEntity<Map<String, String>> translate(@RequestBody TranslateRequest request) {
        String target = (request.getTarget() == null || request.getTarget().isBlank())
                ? "de"
                : request.getTarget();
        return ResponseEntity.ok(translationService.translateBatch(request.getTexts(), target));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("enabled", translationService.isEnabled()));
    }
}
