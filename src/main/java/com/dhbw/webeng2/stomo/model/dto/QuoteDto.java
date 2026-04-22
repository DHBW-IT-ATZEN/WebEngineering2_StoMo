package com.dhbw.webeng2.stomo.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuoteDto {
    private String date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
}
