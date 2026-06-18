package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistItemRepo extends JpaRepository<WatchlistItem, Long> {

    boolean existsByWatchlistIdAndSymbol(Long watchlistId, String symbol);

    Optional<WatchlistItem> findByIdAndWatchlistId(Long id, Long watchlistId);
}
