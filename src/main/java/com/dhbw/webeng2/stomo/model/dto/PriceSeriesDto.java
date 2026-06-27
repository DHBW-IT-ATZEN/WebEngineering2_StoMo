package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Both cached granularities for a symbol. The client resamples these into the views:
 * intraday from {@code fine} (10-min), weekly/monthly from {@code coarse} (30-min).
 */
@Data
@Builder
public class PriceSeriesDto {
    private String symbol;
    private String currency;       // native listing currency from Yahoo (e.g. USD, JPY, EUR)
    private String type;           // Yahoo instrumentType: EQUITY, INDEX, ETF, CURRENCY, …
    private List<QuoteDto> coarse; // 30-minute bars (~60 days)
    private List<QuoteDto> fine;   // 10-minute bars (recent days)
}
