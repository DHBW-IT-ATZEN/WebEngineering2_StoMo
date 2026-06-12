package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.WatchlistItemDto;
import com.dhbw.webeng2.stomo.model.entity.PortfolioItem;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.repository.PortfolioRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Watchlist operations for the current user. Prices (starting + current) come from the
 * DB-cached Yahoo series via {@link PriceHistoryService}, so no paid API quota is spent
 * and the feature works even when the live-quote APIs are disabled.
 */
@Service
public class PortfolioService {

    private final PortfolioRepo repo;
    private final PriceHistoryService priceHistory;

    public PortfolioService(PortfolioRepo repo, PriceHistoryService priceHistory) {
        this.repo = repo;
        this.priceHistory = priceHistory;
    }

    public WatchlistItemDto add(User user, String rawSymbol) {
        String symbol = rawSymbol == null ? "" : rawSymbol.trim().toUpperCase();
        if (symbol.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Symbol is required");
        }
        if (repo.existsByUserIdAndSymbol(user.getId(), symbol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, symbol + " is already in your watchlist");
        }
        Double startPrice = safeLatestPrice(symbol);
        if (startPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not fetch a price for " + symbol + " right now. Please try again.");
        }
        PortfolioItem item = PortfolioItem.builder()
                .user(user)
                .symbol(symbol)
                .startPrice(startPrice)
                .addedAt(Instant.now())
                .build();
        return toDto(repo.save(item), startPrice);
    }

    public List<WatchlistItemDto> list(User user) {
        return repo.findByUserId(user.getId()).stream()
                .map(item -> toDto(item, safeLatestPrice(item.getSymbol())))
                .toList();
    }

    public void remove(User user, Long id) {
        PortfolioItem item = repo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));
        repo.delete(item);
    }

    private Double safeLatestPrice(String symbol) {
        try {
            return priceHistory.getLatestPrice(symbol);
        } catch (RuntimeException ex) {
            return null; // transient upstream failure — show the row without a current price
        }
    }

    private WatchlistItemDto toDto(PortfolioItem item, Double currentPrice) {
        Double start = item.getStartPrice();
        Double changeAbs = null;
        Double changePct = null;
        if (start != null && currentPrice != null) {
            changeAbs = currentPrice - start;
            if (start != 0d) {
                changePct = (currentPrice - start) / start * 100;
            }
        }
        return WatchlistItemDto.builder()
                .id(item.getId())
                .symbol(item.getSymbol())
                .startPrice(start)
                .currentPrice(currentPrice)
                .changeAbs(changeAbs)
                .changePct(changePct)
                .addedAt(item.getAddedAt())
                .build();
    }
}
