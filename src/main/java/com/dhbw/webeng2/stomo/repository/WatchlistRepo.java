package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepo extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Watchlist> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
