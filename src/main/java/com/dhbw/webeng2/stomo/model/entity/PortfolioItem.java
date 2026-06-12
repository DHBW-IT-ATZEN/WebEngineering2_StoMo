package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A watchlist entry: one symbol a user is tracking, plus the price captured when it was
 * added ("starting price") so performance can be measured from that point. Unique per
 * (user, symbol) — a user can't track the same symbol twice.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "portfolio_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
public class PortfolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    /** Price at the moment the symbol was added to the watchlist. */
    private Double startPrice;

    /** When the symbol was added. */
    private Instant addedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
