package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** A watchlist with its items (each already enriched with current price/performance). */
@Data
@Builder
public class WatchlistDto {
    private Long id;
    private String name;
    private Instant createdAt;
    private List<WatchlistItemDto> items;
}
