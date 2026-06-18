package com.dhbw.webeng2.stomo.service;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.WatchlistDto;
import com.dhbw.webeng2.stomo.model.dto.WatchlistItemDto;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.entity.Watchlist;
import com.dhbw.webeng2.stomo.model.entity.WatchlistItem;
import com.dhbw.webeng2.stomo.repository.WatchlistItemRepo;
import com.dhbw.webeng2.stomo.repository.WatchlistRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the watchlist business logic, with all collaborators mocked. Covers the
 * happy paths plus the validation, conflict and not-found branches and the status codes
 * they map to.
 */
@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepo watchlistRepo;
    @Mock
    private WatchlistItemRepo itemRepo;
    @Mock
    private PriceHistoryService priceHistory;
    @InjectMocks
    private WatchlistService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void listCreatesDefaultWatchlistWhenUserHasNone() {
        when(watchlistRepo.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(watchlistRepo.save(any(Watchlist.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        List<WatchlistDto> result = service.list(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Watchlist");
        assertThat(result.get(0).getItems()).isEmpty();
    }

    @Test
    void createTrimsNameAndPersists() {
        when(watchlistRepo.existsByUserIdAndName(1L, "Tech")).thenReturn(false);
        when(watchlistRepo.save(any(Watchlist.class))).thenAnswer(inv -> withId(inv.getArgument(0), 5L));

        WatchlistDto dto = service.create(user, "  Tech  ");

        assertThat(dto.getName()).isEqualTo("Tech");
        assertThat(dto.getId()).isEqualTo(5L);
    }

    @Test
    void createRejectsDuplicateName() {
        when(watchlistRepo.existsByUserIdAndName(1L, "Tech")).thenReturn(true);

        assertThatThrownBy(() -> service.create(user, "Tech"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void renameUpdatesName() {
        Watchlist list = Watchlist.builder().id(5L).name("Old").user(user).createdAt(Instant.now()).build();
        when(watchlistRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(list));
        when(watchlistRepo.existsByUserIdAndName(1L, "New")).thenReturn(false);
        when(watchlistRepo.save(list)).thenReturn(list);

        WatchlistDto dto = service.rename(user, 5L, "New");

        assertThat(dto.getName()).isEqualTo("New");
    }

    @Test
    void renameRejectsUnknownWatchlist() {
        when(watchlistRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rename(user, 9L, "New"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemRejectsBlankSymbol() {
        assertThatThrownBy(() -> service.addItem(user, 5L, "   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void addItemRejectsDuplicateSymbol() {
        when(watchlistRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(listWithId(5L)));
        when(itemRepo.existsByWatchlistIdAndSymbol(5L, "AAPL")).thenReturn(true);

        assertThatThrownBy(() -> service.addItem(user, 5L, "aapl"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void addItemFailsWhenPriceUnavailable() {
        when(watchlistRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(listWithId(5L)));
        when(itemRepo.existsByWatchlistIdAndSymbol(5L, "AAPL")).thenReturn(false);
        when(priceHistory.getLatestPrice("AAPL")).thenThrow(new RuntimeException("upstream down"));

        assertThatThrownBy(() -> service.addItem(user, 5L, "aapl"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void addItemStoresStartPriceAndUppercasesSymbol() {
        when(watchlistRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(listWithId(5L)));
        when(itemRepo.existsByWatchlistIdAndSymbol(5L, "AAPL")).thenReturn(false);
        when(priceHistory.getLatestPrice("AAPL")).thenReturn(150.0);
        when(itemRepo.save(any(WatchlistItem.class))).thenAnswer(inv -> {
            WatchlistItem item = inv.getArgument(0);
            item.setId(7L);
            return item;
        });

        WatchlistItemDto dto = service.addItem(user, 5L, "aapl");

        assertThat(dto.getSymbol()).isEqualTo("AAPL");
        assertThat(dto.getStartPrice()).isEqualTo(150.0);
        assertThat(dto.getCurrentPrice()).isEqualTo(150.0);
        assertThat(dto.getChangeAbs()).isEqualTo(0.0);
    }

    @Test
    void removeItemRejectsUnknownItem() {
        when(watchlistRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(listWithId(5L)));
        when(itemRepo.findByIdAndWatchlistId(8L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem(user, 5L, 8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Watchlist listWithId(long id) {
        return Watchlist.builder().id(id).name("Tech").user(user).createdAt(Instant.now()).build();
    }

    private static Watchlist withId(Watchlist list, long id) {
        list.setId(id);
        return list;
    }
}
