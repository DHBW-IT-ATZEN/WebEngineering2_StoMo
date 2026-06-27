package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

/** One entry in the homepage ticker banner: a symbol with its latest price and day change. */
@Data
@Builder
public class TickerDto {
    private String symbol;
    private Double price;
    private Double changePct;
    private String currency; // native listing currency from Yahoo (e.g. USD, JPY, EUR)
    private String type;     // Yahoo instrumentType: EQUITY, INDEX, ETF, CURRENCY, …
}
