package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MarketDataResponseDto {
    private String symbol;
    private String interval; // z.B. "DAILY" oder "WEEKLY"
    private List<QuoteDto> history;
}
