package com.dhbw.webeng2.stomo.repository;

import com.dhbw.webeng2.stomo.model.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepo extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByUserId(Long userId);

    boolean existsByUserIdAndSymbol(Long userId, String symbol);

    Optional<PortfolioItem> findByIdAndUserId(Long id, Long userId);
}
