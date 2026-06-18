package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single symbol inside a {@link Watchlist}, plus the price captured when it was added
 * ("starting price") so performance can be measured from that point. Unique per
 * (watchlist, symbol) — the same symbol can't be added to one list twice.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "watchlist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"watchlist_id", "symbol"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    /** Price at the moment the symbol was added to the watchlist. */
    private Double startPrice;

    /** When the symbol was added. */
    private Instant addedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;
}
