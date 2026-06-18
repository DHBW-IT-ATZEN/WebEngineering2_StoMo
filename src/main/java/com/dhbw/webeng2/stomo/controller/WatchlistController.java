package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.AddSymbolRequest;
import com.dhbw.webeng2.stomo.model.dto.CreateWatchlistRequest;
import com.dhbw.webeng2.stomo.model.dto.UpdateWatchlistRequest;
import com.dhbw.webeng2.stomo.model.dto.WatchlistDto;
import com.dhbw.webeng2.stomo.model.dto.WatchlistItemDto;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import com.dhbw.webeng2.stomo.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The current user's watchlists. All endpoints require a valid bearer token; the user is
 * resolved from the token subject (email). Provides full CRUD on watchlists plus add/remove
 * of the symbols they contain.
 */
@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final UserRepo userRepo;

    @GetMapping
    public ResponseEntity<List<WatchlistDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(watchlistService.list(currentUser(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchlistDto> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(watchlistService.get(currentUser(jwt), id));
    }

    @PostMapping
    public ResponseEntity<WatchlistDto> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateWatchlistRequest request) {
        WatchlistDto created = watchlistService.create(currentUser(jwt), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WatchlistDto> rename(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody UpdateWatchlistRequest request) {
        return ResponseEntity.ok(watchlistService.rename(currentUser(jwt), id, request.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        watchlistService.delete(currentUser(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<WatchlistItemDto> addItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                    @Valid @RequestBody AddSymbolRequest request) {
        WatchlistItemDto item = watchlistService.addItem(currentUser(jwt), id, request.getSymbol());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                           @PathVariable Long itemId) {
        watchlistService.removeItem(currentUser(jwt), id, itemId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Jwt jwt) {
        return userRepo.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
