package com.dhbw.webeng2.stomo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDto {
    private String date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;
}
