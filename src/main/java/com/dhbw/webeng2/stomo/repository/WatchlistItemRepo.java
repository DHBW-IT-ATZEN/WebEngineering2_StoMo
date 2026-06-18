package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.WatchlistItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepo extends JpaRepository<WatchlistItem, Long> {

    boolean existsByWatchlistIdAndSymbol(Long watchlistId, String symbol);

    Optional<WatchlistItem> findByIdAndWatchlistId(Long id, Long watchlistId);

    /** How many distinct symbols are watched across all users' watchlists. */
    @Query("SELECT COUNT(DISTINCT i.symbol) FROM WatchlistItem i")
    long countDistinctSymbols();

    /** Symbols ordered by how many watchlist entries reference them (most-watched first). */
    @Query("SELECT i.symbol FROM WatchlistItem i GROUP BY i.symbol ORDER BY COUNT(i) DESC")
    List<String> findTopSymbols(Pageable pageable);
}
