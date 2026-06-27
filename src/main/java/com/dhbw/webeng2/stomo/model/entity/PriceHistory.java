package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Cached intraday (30-minute) price series for one symbol. The symbol is the
 * primary key, so a given security can never be stored twice. Intraday/weekly/
 * monthly views are all derived from this one series on the client.
 */
@Getter
@Setter
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @Column(length = 32)
    private String symbol;

    // No @Lob: on PostgreSQL that maps a String to a Large Object (oid), and the driver then
    // tries to read this text column as a long — "Bad value for type long". Plain text works
    // across PostgreSQL, H2 and MySQL.
    @Column(name = "bars_json", columnDefinition = "text", nullable = false)
    private String barsJson; // 30-minute series

    @Column(name = "fine_bars_json", columnDefinition = "text")
    private String fineBarsJson; // 10-minute series (nullable so pre-existing rows simply refetch)

    @Column(length = 16)
    private String currency; // native listing currency from Yahoo (e.g. USD, JPY, EUR)

    @Column(length = 24)
    private String type; // Yahoo instrumentType: EQUITY, INDEX, ETF, CURRENCY, …

    @Column(nullable = false)
    private Instant fetchedAt;

    /** Optimistic-locking version — coordinates concurrent refreshes across backend instances. */
    @Version
    private Long version;
}
