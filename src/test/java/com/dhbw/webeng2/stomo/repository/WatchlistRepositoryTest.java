package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.model.entity.Watchlist;
import com.dhbw.webeng2.stomo.model.entity.WatchlistItem;
import com.dhbw.webeng2.stomo.model.enums.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence-layer (integration) tests for the watchlist repositories against H2, covering
 * the User -> Watchlist -> WatchlistItem relationships, ownership scoping, the unique
 * constraints and cascade delete. Each test runs in its own transaction and rolls back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WatchlistRepositoryTest {

    @Autowired
    private WatchlistRepo watchlistRepo;
    @Autowired
    private WatchlistItemRepo itemRepo;
    @Autowired
    private UserRepo userRepo;
    @PersistenceContext
    private EntityManager em;

    @Test
    void findsWatchlistsForOwnerOrderedByCreatedAt() {
        User user = newUser("a@example.com");
        watchlistRepo.save(watchlist(user, "First", Instant.parse("2024-01-01T00:00:00Z")));
        watchlistRepo.save(watchlist(user, "Second", Instant.parse("2024-02-01T00:00:00Z")));
        em.flush();
        em.clear();

        List<Watchlist> lists = watchlistRepo.findByUserIdOrderByCreatedAtAsc(user.getId());

        assertThat(lists).extracting(Watchlist::getName).containsExactly("First", "Second");
    }

    @Test
    void scopesWatchlistLookupToOwner() {
        User owner = newUser("owner@example.com");
        User other = newUser("other@example.com");
        Watchlist list = watchlistRepo.save(watchlist(owner, "Mine", Instant.now()));
        em.flush();

        assertThat(watchlistRepo.findByIdAndUserId(list.getId(), owner.getId())).isPresent();
        assertThat(watchlistRepo.findByIdAndUserId(list.getId(), other.getId())).isEmpty();
    }

    @Test
    void enforcesUniqueListNamePerUser() {
        User user = newUser("dup@example.com");
        watchlistRepo.saveAndFlush(watchlist(user, "Tech", Instant.now()));

        Watchlist duplicate = watchlist(user, "Tech", Instant.now());

        assertThatThrownBy(() -> watchlistRepo.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascadeDeletesItemsWithWatchlist() {
        User user = newUser("cascade@example.com");
        Watchlist list = watchlistRepo.save(watchlist(user, "Tech", Instant.now()));
        itemRepo.save(item(list, "AAPL"));
        em.flush();
        em.clear();

        watchlistRepo.delete(watchlistRepo.findById(list.getId()).orElseThrow());
        em.flush();

        assertThat(itemRepo.count()).isZero();
    }

    @Test
    void itemLookupsAreScopedToWatchlist() {
        User user = newUser("items@example.com");
        Watchlist list = watchlistRepo.save(watchlist(user, "Tech", Instant.now()));
        WatchlistItem stored = itemRepo.save(item(list, "MSFT"));
        em.flush();

        assertThat(itemRepo.existsByWatchlistIdAndSymbol(list.getId(), "MSFT")).isTrue();
        assertThat(itemRepo.existsByWatchlistIdAndSymbol(list.getId(), "TSLA")).isFalse();
        assertThat(itemRepo.findByIdAndWatchlistId(stored.getId(), list.getId())).isPresent();
    }

    private User newUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstname("Jane");
        user.setLastname("Doe");
        user.setPassword("hash");
        user.setStatus(Status.ACTIVE);
        return userRepo.save(user);
    }

    private static Watchlist watchlist(User user, String name, Instant createdAt) {
        return Watchlist.builder().user(user).name(name).createdAt(createdAt).build();
    }

    private static WatchlistItem item(Watchlist list, String symbol) {
        return WatchlistItem.builder().watchlist(list).symbol(symbol).startPrice(1.0).addedAt(Instant.now()).build();
    }
}
