package com.dhbw.webeng2.stomo.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A named collection of symbols a user is tracking (e.g. "Tech", "Dividends"). A user can
 * keep several. Deleting a watchlist cascades to its items. Unique per (user, name) so a
 * user can't have two lists with the same name.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "watchlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    @Builder.Default
    private List<WatchlistItem> items = new ArrayList<>();
}
