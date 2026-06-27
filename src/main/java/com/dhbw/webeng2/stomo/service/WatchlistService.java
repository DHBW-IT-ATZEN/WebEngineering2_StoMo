package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.WatchlistDto;
import com.dhbw.webeng2.stomo.model.dto.WatchlistItemDto;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.entity.Watchlist;
import com.dhbw.webeng2.stomo.model.entity.WatchlistItem;
import com.dhbw.webeng2.stomo.repository.WatchlistItemRepo;
import com.dhbw.webeng2.stomo.repository.WatchlistRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Watchlist operations for the current user: full CRUD on watchlists plus add/remove of the
 * symbols inside them. Prices (starting + current) come from the DB-cached Yahoo series via
 * {@link PriceHistoryService}, so no paid API quota is spent and the feature works even when
 * the live-quote APIs have no key. Every method scopes by the owning user, so one user can
 * never see or touch another's lists.
 */
@Service
@Transactional
public class WatchlistService {

    private static final String DEFAULT_NAME = "My Watchlist";

    private final WatchlistRepo watchlistRepo;
    private final WatchlistItemRepo itemRepo;
    private final PriceHistoryService priceHistory;

    public WatchlistService(WatchlistRepo watchlistRepo, WatchlistItemRepo itemRepo,
                            PriceHistoryService priceHistory) {
        this.watchlistRepo = watchlistRepo;
        this.itemRepo = itemRepo;
        this.priceHistory = priceHistory;
    }

    /** Every user always has at least one list, so the UI is never empty. */
    public List<WatchlistDto> list(User user) {
        List<Watchlist> lists = watchlistRepo.findByUserIdOrderByCreatedAtAsc(user.getId());
        if (lists.isEmpty()) {
            lists = List.of(createEntity(user, DEFAULT_NAME));
        }
        return lists.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public WatchlistDto get(User user, Long id) {
        return toDto(require(user, id));
    }

    public WatchlistDto create(User user, String name) {
        String clean = requireName(name);
        if (watchlistRepo.existsByUserIdAndName(user.getId(), clean)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a list named \"" + clean + "\"");
        }
        return toDto(createEntity(user, clean));
    }

    public WatchlistDto rename(User user, Long id, String name) {
        String clean = requireName(name);
        Watchlist list = require(user, id);
        if (!clean.equals(list.getName()) && watchlistRepo.existsByUserIdAndName(user.getId(), clean)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a list named \"" + clean + "\"");
        }
        list.setName(clean);
        return toDto(watchlistRepo.save(list));
    }

    public void delete(User user, Long id) {
        watchlistRepo.delete(require(user, id));
    }

    public WatchlistItemDto addItem(User user, Long watchlistId, String rawSymbol) {
        String symbol = rawSymbol == null ? "" : rawSymbol.trim().toUpperCase();
        if (symbol.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Symbol is required");
        }
        Watchlist list = require(user, watchlistId);
        if (itemRepo.existsByWatchlistIdAndSymbol(list.getId(), symbol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, symbol + " is already in this list");
        }
        Double startPrice = safeLatestPrice(symbol);
        if (startPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not fetch a price for " + symbol + " right now. Please try again.");
        }
        WatchlistItem item = WatchlistItem.builder()
                .watchlist(list)
                .symbol(symbol)
                .startPrice(startPrice)
                .currency(safeCurrency(symbol))
                .type(safeType(symbol))
                .addedAt(Instant.now())
                .build();
        return toItemDto(itemRepo.save(item), startPrice);
    }

    public void removeItem(User user, Long watchlistId, Long itemId) {
        Watchlist list = require(user, watchlistId);
        WatchlistItem item = itemRepo.findByIdAndWatchlistId(itemId, list.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));
        itemRepo.delete(item);
    }

    // --- helpers ---

    private Watchlist createEntity(User user, String name) {
        Watchlist list = Watchlist.builder()
                .user(user)
                .name(name)
                .createdAt(Instant.now())
                .build();
        return watchlistRepo.save(list);
    }

    private Watchlist require(User user, Long id) {
        return watchlistRepo.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found"));
    }

    private static String requireName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        return clean;
    }

    private Double safeLatestPrice(String symbol) {
        try {
            return priceHistory.getLatestPrice(symbol);
        } catch (RuntimeException ex) {
            return null; // transient upstream failure — show the row without a current price
        }
    }

    private String safeCurrency(String symbol) {
        try {
            return priceHistory.getCurrency(symbol);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String safeType(String symbol) {
        try {
            return priceHistory.getType(symbol);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private WatchlistDto toDto(Watchlist list) {
        List<WatchlistItemDto> items = list.getItems().stream()
                .map(item -> toItemDto(item, safeLatestPrice(item.getSymbol())))
                .toList();
        return WatchlistDto.builder()
                .id(list.getId())
                .name(list.getName())
                .createdAt(list.getCreatedAt())
                .items(items)
                .build();
    }

    private WatchlistItemDto toItemDto(WatchlistItem item, Double currentPrice) {
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
                .currency(item.getCurrency())
                .type(item.getType())
                .startPrice(start)
                .currentPrice(currentPrice)
                .changeAbs(changeAbs)
                .changePct(changePct)
                .addedAt(item.getAddedAt())
                .build();
    }
}
