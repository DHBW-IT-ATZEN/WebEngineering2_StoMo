package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.model.dto.YodaTranslateRequest;
import com.dhbw.webeng2.stomo.service.YodaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/yoda")
@RequiredArgsConstructor
public class YodaController {
    private final YodaService yodaService;

    @GetMapping("/translate")
    public ResponseEntity<String> getTranslation(@RequestParam String text) {
        return ResponseEntity.ok(yodaService.translateOne(text));
    }

    /** Batch translate: { "texts": [...] } -> { "<source>": "<translated>", ... }. */
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translateBatch(@RequestBody YodaTranslateRequest request) {
        return ResponseEntity.ok(yodaService.translateBatch(request.getTexts()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("enabled", yodaService.isEnabled()));
    }
}
