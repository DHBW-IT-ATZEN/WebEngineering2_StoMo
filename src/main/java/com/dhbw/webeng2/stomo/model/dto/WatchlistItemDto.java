package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** A watchlist row with performance measured from the starting price. */
@Data
@Builder
public class WatchlistItemDto {
    private Long id;
    private String symbol;
    private Double startPrice;
    private Double currentPrice;
    private Double changeAbs;
    private Double changePct;
    private Instant addedAt;
}
