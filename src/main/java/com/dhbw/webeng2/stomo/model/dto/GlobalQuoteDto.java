package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GlobalQuoteDto {
    private String symbol;
    private Double price;
    private Double change;
    private Double changePercent;
    private Double open;
    private Double high;
    private Double low;
    private Double previousClose;
    private String latestTradingDay;
}
