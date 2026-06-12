package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.WatchlistItemDto;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import com.dhbw.webeng2.stomo.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The current user's watchlist. All endpoints require a valid bearer token; the user is
 * resolved from the token's subject (email).
 */
@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepo userRepo;

    @GetMapping
    public ResponseEntity<List<WatchlistItemDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(portfolioService.list(currentUser(jwt)));
    }

    @PostMapping
    public ResponseEntity<WatchlistItemDto> add(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody Map<String, String> body) {
        WatchlistItemDto item = portfolioService.add(currentUser(jwt), body.get("symbol"));
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        portfolioService.remove(currentUser(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Jwt jwt) {
        return userRepo.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
