package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerformanceDto {
    private String symbol;
    private Double currentPrice;
    private Double totalInvestment;
    private Double currentTotalValue;
    private Double performancePercent;
}
